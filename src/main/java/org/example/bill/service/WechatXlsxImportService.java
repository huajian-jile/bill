package org.example.bill.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.bill.domain.*;
import org.example.bill.repo.*;
import org.example.bill.util.PhoneUtil;
import org.example.bill.util.RowHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WechatXlsxImportService {

    private static final Pattern NICK = Pattern.compile("微信昵称[：:]\\s*\\[([^]]+)]");
    private static final Pattern RANGE =
            Pattern.compile("起始时间[：:]\\s*\\[([^]]+)]\\s*终止时间[：:]\\s*\\[([^]]+)]");
    private static final Pattern EXPORT_TYPE = Pattern.compile("导出类型[：:]\\s*\\[([^]]+)]");
    private static final Pattern EXPORT_TIME = Pattern.compile("导出时间[：:]\\s*\\[([^]]+)]");
    private static final Pattern TOTAL = Pattern.compile("共\\s*(\\d+)\\s*笔记录");
    private static final Pattern INCOME =
            Pattern.compile("收入[：:]\\s*(\\d+)\\s*笔\\s*([\\d.]+)\\s*元");
    private static final Pattern EXPENSE =
            Pattern.compile("支出[：:]\\s*(\\d+)\\s*笔\\s*([\\d.]+)\\s*元");
    private static final Pattern NEUTRAL =
            Pattern.compile("中性[^：\\n]*[：:]\\s*(\\d+)\\s*笔\\s*([\\d.]+)\\s*元");

    private final WechatUserRepository wechatUserRepo;
    private final WechatBillImportRepository importRepo;
    private final WechatBillTransactionRepository txRepo;

    /** 仅支持 xlsx，须传中国大陆手机号 */
    @Transactional
    public WechatBillImport importWechat(MultipartFile file, String mobileCn) throws Exception {
        PhoneUtil.requireValidCnMobile(mobileCn);
        String fn = file.getOriginalFilename();
        if (fn != null && fn.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("微信账单仅支持 xlsx 格式，不支持 CSV");
        }
        return importXlsx(file, mobileCn);
    }

    @Transactional
    public WechatBillImport importXlsx(MultipartFile file, String mobileCn) throws Exception {
        Map<String, Object> meta = new HashMap<>();
        List<String[]> dataRows = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        try (InputStream in = file.getInputStream();
                Workbook wb = new XSSFWorkbook(in)) {
            Sheet sh = wb.getSheetAt(0);
            boolean headerFound = false;
            for (Row row : sh) {
                String line = rowText(row);
                if (!headerFound) {
                    parseMetaLine(line, meta);
                    if (looksLikeHeader(row)) {
                        headerFound = true;
                        headers.clear();
                        for (int c = 0; c < row.getLastCellNum(); c++) {
                            headers.add(cellStr(row.getCell(c)).trim());
                        }
                        continue;
                    }
                    continue;
                }
                if (isEmptyRow(row)) {
                    continue;
                }
                String[] vals = new String[headers.size()];
                Arrays.fill(vals, "");
                for (int i = 0; i < headers.size(); i++) {
                    vals[i] = cellStr(row.getCell(i)).trim();
                }
                dataRows.add(vals);
            }
        }

        if (headers.isEmpty()) {
            throw new IllegalArgumentException("未找到微信账单表头行");
        }

        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (!headers.get(i).isBlank()) {
                col.put(headers.get(i), i);
            }
        }

        String nickname =
                Optional.ofNullable(meta.get("wechat_nickname"))
                        .map(Object::toString)
                        .orElse("未知用户");
        WechatUser wu =
                wechatUserRepo
                        .findTopByWechatNicknameOrderByIdAsc(nickname)
                        .orElseGet(
                                () -> {
                                    WechatUser u = new WechatUser();
                                    u.setWechatNickname(nickname);
                                    Instant n = Instant.now();
                                    u.setCreatedAt(n);
                                    u.setUpdatedAt(n);
                                    u.setArchived(false);
                                    return wechatUserRepo.save(u);
                                });

        WechatBillImport imp = new WechatBillImport();
        imp.setUserId(wu.getId());
        imp.setPersonId(wu.getPersonId());
        imp.setPhoneId(wu.getPhoneId());
        imp.setMobileCn(PhoneUtil.normalizeCnMobile(mobileCn));
        imp.setSourceFile(Objects.requireNonNullElse(file.getOriginalFilename(), "upload.xlsx"));
        imp.setExportType(str(meta.get("export_type")));
        imp.setExportTime(parseInstant(meta.get("export_time")));
        imp.setRangeStart(parseInstant(meta.get("range_start")));
        imp.setRangeEnd(parseInstant(meta.get("range_end")));
        if (meta.get("total_count") != null) {
            imp.setTotalCount(((Number) meta.get("total_count")).intValue());
        }
        if (meta.get("income_count") != null) {
            imp.setIncomeCount(((Number) meta.get("income_count")).intValue());
        }
        if (meta.get("income_amount") != null) {
            imp.setIncomeAmount(new BigDecimal(meta.get("income_amount").toString()));
        }
        if (meta.get("expense_count") != null) {
            imp.setExpenseCount(((Number) meta.get("expense_count")).intValue());
        }
        if (meta.get("expense_amount") != null) {
            imp.setExpenseAmount(new BigDecimal(meta.get("expense_amount").toString()));
        }
        if (meta.get("neutral_count") != null) {
            imp.setNeutralCount(((Number) meta.get("neutral_count")).intValue());
        }
        if (meta.get("neutral_amount") != null) {
            imp.setNeutralAmount(new BigDecimal(meta.get("neutral_amount").toString()));
        }
        Instant now = Instant.now();
        imp.setCreatedAt(now);
        imp.setUpdatedAt(now);
        imp.setArchived(false);
        imp.setCreatedBy("spring-import");
        imp.setUpdatedBy("spring-import");
        imp = importRepo.save(imp);

        for (String[] vals : dataRows) {
            WechatBillTransaction t = new WechatBillTransaction();
            t.setBillImportId(imp.getId());
            t.setTradeTime(get(col, vals, "交易时间"));
            t.setTradeType(get(col, vals, "交易类型"));
            t.setCounterparty(get(col, vals, "交易对方"));
            t.setProduct(get(col, vals, "商品"));
            t.setIncomeExpense(get(col, vals, "收/支"));
            t.setAmountYuan(parseAmt(get(col, vals, "金额(元)")));
            t.setPaymentMethod(get(col, vals, "支付方式"));
            t.setStatus(get(col, vals, "当前状态"));
            t.setTradeNo(get(col, vals, "交易单号"));
            t.setMerchantNo(get(col, vals, "商户单号"));
            t.setRemark(get(col, vals, "备注"));
            t.setSourceFile(imp.getSourceFile());
            t.setPersonId(wu.getPersonId());
            t.setPhoneId(wu.getPhoneId());
            t.setMobileCn(PhoneUtil.normalizeCnMobile(mobileCn));
            t.setRowHash(
                    RowHashUtil.hash(
                            t.getTradeTime(),
                            t.getTradeNo(),
                            t.getMerchantNo(),
                            t.getAmountYuan(),
                            t.getTradeType(),
                            t.getCounterparty()));
            t.setCreatedAt(now);
            t.setUpdatedAt(now);
            t.setCreatedBy("spring-import");
            t.setUpdatedBy("spring-import");
            t.setArchived(false);
            persistWechatTransaction(t, wu, imp, now);
        }
        return imp;
    }

    public WechatBillImport importCsv(MultipartFile file, String mobileCn) throws Exception {
        PhoneUtil.requireValidCnMobile(mobileCn);
        List<String[]> dataRows = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            CSVFormat fmt = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();
            try (CSVParser parser = fmt.parse(reader)) {
                headers = new ArrayList<>(parser.getHeaderNames());
                for (CSVRecord rec : parser) {
                    String[] vals = new String[headers.size()];
                    for (int i = 0; i < headers.size(); i++) {
                        try {
                            vals[i] = rec.get(i) != null ? rec.get(i).trim() : "";
                        } catch (IllegalArgumentException ex) {
                            vals[i] = "";
                        }
                    }
                    dataRows.add(vals);
                }
            }
        }
        if (headers.isEmpty()) {
            throw new IllegalArgumentException("CSV 无表头");
        }
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (!headers.get(i).isBlank()) {
                col.put(headers.get(i).trim(), i);
            }
        }
        String nick = "账单用户-" + PhoneUtil.normalizeCnMobile(mobileCn);
        WechatUser wu =
                wechatUserRepo
                        .findTopByWechatNicknameOrderByIdAsc(nick)
                        .orElseGet(
                                () -> {
                                    WechatUser u = new WechatUser();
                                    u.setWechatNickname(nick);
                                    Instant n = Instant.now();
                                    u.setCreatedAt(n);
                                    u.setUpdatedAt(n);
                                    u.setArchived(false);
                                    return wechatUserRepo.save(u);
                                });

        WechatBillImport imp = new WechatBillImport();
        imp.setUserId(wu.getId());
        imp.setPersonId(wu.getPersonId());
        imp.setPhoneId(wu.getPhoneId());
        imp.setMobileCn(PhoneUtil.normalizeCnMobile(mobileCn));
        imp.setSourceFile(Objects.requireNonNullElse(file.getOriginalFilename(), "upload.csv"));
        Instant now = Instant.now();
        imp.setCreatedAt(now);
        imp.setUpdatedAt(now);
        imp.setArchived(false);
        imp.setCreatedBy("spring-import");
        imp.setUpdatedBy("spring-import");
        imp = importRepo.save(imp);

        for (String[] vals : dataRows) {
            WechatBillTransaction t = new WechatBillTransaction();
            t.setBillImportId(imp.getId());
            t.setTradeTime(get(col, vals, "交易时间"));
            t.setTradeType(get(col, vals, "交易类型"));
            t.setCounterparty(get(col, vals, "交易对方"));
            t.setProduct(get(col, vals, "商品"));
            t.setIncomeExpense(firstNonBlank(get(col, vals, "收/支"), get(col, vals, "收支")));
            t.setAmountYuan(parseAmt(get(col, vals, "金额(元)")));
            t.setPaymentMethod(firstNonBlank(get(col, vals, "支付方式"), get(col, vals, "收付款方式")));
            t.setStatus(firstNonBlank(get(col, vals, "当前状态"), get(col, vals, "交易状态")));
            t.setTradeNo(firstNonBlank(get(col, vals, "交易单号"), get(col, vals, "交易订单号")));
            t.setMerchantNo(firstNonBlank(get(col, vals, "商户单号"), get(col, vals, "商家订单号")));
            t.setRemark(get(col, vals, "备注"));
            t.setSourceFile(imp.getSourceFile());
            t.setPersonId(wu.getPersonId());
            t.setPhoneId(wu.getPhoneId());
            t.setMobileCn(PhoneUtil.normalizeCnMobile(mobileCn));
            t.setRowHash(
                    RowHashUtil.hash(
                            t.getTradeTime(),
                            t.getTradeNo(),
                            t.getMerchantNo(),
                            t.getAmountYuan(),
                            t.getTradeType(),
                            t.getCounterparty()));
            t.setCreatedAt(now);
            t.setUpdatedAt(now);
            t.setCreatedBy("spring-import");
            t.setUpdatedBy("spring-import");
            t.setArchived(false);
            persistWechatTransaction(t, wu, imp, now);
        }
        return imp;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return b == null ? "" : b;
    }

    /**
     * 先按 (person_id, row_hash) 查找，再按 (mobile_cn, row_hash)，避免未绑定 person 时重复插入。
     */
    private void persistWechatTransaction(
            WechatBillTransaction t, WechatUser wu, WechatBillImport imp, Instant now) {
        String hash = t.getRowHash();
        if (hash == null || hash.isBlank()) {
            txRepo.save(t);
            return;
        }
        Optional<WechatBillTransaction> existing = Optional.empty();
        Long pid = wu.getPersonId();
        if (pid != null) {
            existing = txRepo.findByPersonIdAndRowHash(pid, hash);
        }
        if (existing.isEmpty()) {
            String mobile = t.getMobileCn();
            if (mobile != null && !mobile.isBlank()) {
                existing = txRepo.findByMobileCnAndRowHash(mobile.trim(), hash);
            }
        }
        if (existing.isPresent()) {
            applyRowToExisting(existing.get(), t, imp.getId(), now);
            txRepo.save(existing.get());
        } else {
            txRepo.save(t);
        }
    }

    private static void applyRowToExisting(
            WechatBillTransaction existing,
            WechatBillTransaction t,
            Long billImportId,
            Instant now) {
        existing.setBillImportId(billImportId);
        existing.setPersonId(t.getPersonId());
        existing.setPhoneId(t.getPhoneId());
        existing.setMobileCn(t.getMobileCn());
        existing.setTradeTime(t.getTradeTime());
        existing.setTradeType(t.getTradeType());
        existing.setCounterparty(t.getCounterparty());
        existing.setProduct(t.getProduct());
        existing.setIncomeExpense(t.getIncomeExpense());
        existing.setAmountYuan(t.getAmountYuan());
        existing.setPaymentMethod(t.getPaymentMethod());
        existing.setStatus(t.getStatus());
        existing.setTradeNo(t.getTradeNo());
        existing.setMerchantNo(t.getMerchantNo());
        existing.setRemark(t.getRemark());
        existing.setSourceFile(t.getSourceFile());
        existing.setUpdatedAt(now);
        existing.setUpdatedBy("spring-import");
    }

    private String get(Map<String, Integer> col, String[] vals, String name) {
        Integer i = col.get(name);
        if (i == null || i >= vals.length) {
            return "";
        }
        return vals[i] == null ? "" : vals[i];
    }

    private void parseMetaLine(String line, Map<String, Object> meta) {
        if (line == null || line.isBlank()) {
            return;
        }
        Matcher m = NICK.matcher(line);
        if (m.find()) {
            meta.put("wechat_nickname", m.group(1).trim());
        }
        m = RANGE.matcher(line);
        if (m.find()) {
            meta.put("range_start", m.group(1).trim());
            meta.put("range_end", m.group(2).trim());
        }
        m = EXPORT_TYPE.matcher(line);
        if (m.find()) {
            meta.put("export_type", m.group(1).trim());
        }
        m = EXPORT_TIME.matcher(line);
        if (m.find()) {
            meta.put("export_time", m.group(1).trim());
        }
        m = TOTAL.matcher(line);
        if (m.find()) {
            meta.put("total_count", Integer.parseInt(m.group(1)));
        }
        m = INCOME.matcher(line);
        if (m.find()) {
            meta.put("income_count", Integer.parseInt(m.group(1)));
            meta.put("income_amount", new BigDecimal(m.group(2)));
        }
        m = EXPENSE.matcher(line);
        if (m.find()) {
            meta.put("expense_count", Integer.parseInt(m.group(1)));
            meta.put("expense_amount", new BigDecimal(m.group(2)));
        }
        m = NEUTRAL.matcher(line);
        if (m.find()) {
            meta.put("neutral_count", Integer.parseInt(m.group(1)));
            meta.put("neutral_amount", new BigDecimal(m.group(2)));
        }
    }

    private boolean looksLikeHeader(Row row) {
        String j = rowText(row);
        return j.contains("交易时间") && (j.contains("交易单号") || j.contains("金额"));
    }

    private String rowText(Row row) {
        StringBuilder sb = new StringBuilder();
        if (row == null) {
            return "";
        }
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) {
                continue;
            }
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                sb.append(
                        LocalDateTime.ofInstant(
                                        cell.getDateCellValue().toInstant(),
                                        ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                sb.append(cellStr(cell));
            }
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String cellStr(Cell c) {
        if (c == null) {
            return "";
        }
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC ->
                    DateUtil.isCellDateFormatted(c)
                            ? LocalDateTime.ofInstant(
                                            c.getDateCellValue().toInstant(),
                                            ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            : BigDecimal.valueOf(c.getNumericCellValue())
                                    .stripTrailingZeros()
                                    .toPlainString();
            case BOOLEAN -> Boolean.toString(c.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !cellStr(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private BigDecimal parseAmt(String s) {
        if (s == null || s.isBlank() || "/".equals(s)) {
            return null;
        }
        try {
            return new BigDecimal(s.replace(",", "").replace("¥", "").replace("元", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private Instant parseInstant(Object o) {
        if (o == null) {
            return null;
        }
        String s = o.toString().trim();
        for (String fmt :
                List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd")) {
            try {
                var fmtter = DateTimeFormatter.ofPattern(fmt);
                if (fmt.contains("HH")) {
                    return LocalDateTime.parse(s, fmtter)
                            .atZone(ZoneId.systemDefault())
                            .toInstant();
                }
                return java.time.LocalDate.parse(s, fmtter)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String str(Object o) {
        return o == null ? null : o.toString();
    }
}
