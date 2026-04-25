package org.example.bill.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
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
import org.example.bill.mapper.WechatBillTransactionMapper;
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
    private static final Pattern EXPORT_TYPE =
            Pattern.compile("(?:导出类型|导出交易类型)[：:]\\s*\\[([^]]+)]");
    private static final Pattern EXPORT_TIME = Pattern.compile("导出时间[：:]\\s*\\[([^]]+)]");
    private static final Pattern TOTAL = Pattern.compile("共\\s*(\\d+)\\s*笔记录");
    private static final Pattern INCOME =
            Pattern.compile("收入[：:]\\s*(\\d+)\\s*笔\\s*([\\d.]+)\\s*元");
    private static final Pattern EXPENSE =
            Pattern.compile("支出[：:]\\s*(\\d+)\\s*笔\\s*([\\d.]+)\\s*元");
    private static final Pattern NEUTRAL =
            Pattern.compile("(?:中性交易|不计收支)[：:]\\s*(\\d+)\\s*笔[，,]?\\s*([\\d.]+)\\s*元");

    private final WechatUserRepository wechatUserRepo;
    private final WechatBillImportRepository importRepo;
    private final WechatBillTransactionRepository txRepo;
    private final WechatBillTransactionMapper txMapper;
    private final BillImportLinkageService billImportLinkageService;

    /** xlsx 或 csv，须传中国大陆手机号 */
    @Transactional
    public WechatBillImport importWechat(MultipartFile file, String mobileCn) throws Exception {
        PhoneUtil.requireValidCnMobile(mobileCn);
        String fn = file.getOriginalFilename();
        if (fn != null && fn.toLowerCase().endsWith(".csv")) {
            return importCsv(file, mobileCn);
        }
        return importXlsx(file, mobileCn);
    }

    /** 支付宝 CSV 账单 */
    @Transactional
    public WechatBillImport importAlipay(MultipartFile file, String mobileCn) throws Exception {
        PhoneUtil.requireValidCnMobile(mobileCn);
        return importCsv(file, mobileCn);
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
                        .findFirstByWechatNicknameAndChannelOrderByIdAsc(nickname, "WECHAT")
                        .orElseGet(
                                () -> {
                                    WechatUser u = new WechatUser();
                                    u.setChannel("WECHAT");
                                    u.setWechatNickname(nickname);
                                    Instant n = Instant.now();
                                    u.setCreatedAt(n);
                                    u.setUpdatedAt(n);
                                    u.setArchived(false);
                                    return wechatUserRepo.save(u);
                                });

        // 建立 person / phone 关联链路
        billImportLinkageService.ensurePhoneAndPersonLinked(wu, mobileCn);
        wu = wechatUserRepo.findById(wu.getId()).orElse(wu);

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

        // 表头行之前逐行读 meta；定位表头后由同一流交给 Commons CSV 解析数据区，避免手拆行导致列错位、引号/逗号/多行单元格被误切。
        Map<String, Object> meta = new LinkedHashMap<>();
        List<String[]> dataRows = new ArrayList<>();
        String[] headerNames;
        char delimiter;
        boolean isWechatFlag = false;

        try (BufferedReader br =
                new BufferedReader(
                        new InputStreamReader(
                                file.getInputStream(), Charset.forName("GBK")))) {
            String line;
            String headerLine = null;
            while ((line = br.readLine()) != null) {
                parseMetaLine(line, meta);
                if (isWechatCsvHeader(line)) {
                    headerLine = line;
                    break;
                }
                if (isAlipayCsvHeader(line)) {
                    headerLine = line;
                    break;
                }
            }
            if (headerLine == null) {
                throw new IllegalArgumentException(
                        "CSV 无表头（微信需含 交易单号；支付宝需含 交易订单号/商家订单号 与 交易时间）");
            }
            final boolean w = isWechatCsvHeader(headerLine);
            if (!w && !isAlipayCsvHeader(headerLine)) {
                throw new IllegalArgumentException("未识别的表头行");
            }
            isWechatFlag = w;
            delimiter = detectCsvDelimiter(headerLine);
            headerNames = parseOneCsvLine(headerLine, delimiter);
            for (int i = 0; i < headerNames.length; i++) {
                headerNames[i] = normalizeHeader(headerNames[i]);
            }
            // 行尾多余逗号会多出一列空表头；Commons CSV 的 setHeader 不允许空名或重名
            headerNames = ensureNonBlankUniqueCsvHeaders(headerNames);

            CSVFormat format =
                    CSVFormat.Builder.create(CSVFormat.RFC4180)
                            .setDelimiter(delimiter)
                            .setHeader(headerNames)
                            .setSkipHeaderRecord(false)
                            .setIgnoreHeaderCase(true)
                            .setTrim(true)
                            .setIgnoreEmptyLines(true)
                            .build();
            try (CSVParser parser = format.parse(br)) {
                for (CSVRecord r : parser) {
                    if (r == null) {
                        continue;
                    }
                    int n = r.size();
                    String[] arr = new String[headerNames.length];
                    for (int c = 0; c < headerNames.length; c++) {
                        if (c < n) {
                            String v = r.get(c);
                            arr[c] = v == null ? "" : v;
                        } else {
                            arr[c] = "";
                        }
                    }
                    dataRows.add(arr);
                }
            }
        }

        final boolean isWechat = isWechatFlag;
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headerNames.length; i++) {
            String h = headerNames[i];
            if (h != null && !h.isBlank()) {
                col.putIfAbsent(h, i);
            }
        }

        String channel = isWechat ? "WECHAT" : "ALIPAY";
        WechatUser wu;
        if (isWechat) {
            String nickname =
                    Optional.ofNullable(meta.get("wechat_nickname"))
                            .map(Object::toString)
                            .orElse("未知用户");
            wu =
                    wechatUserRepo
                            .findFirstByWechatNicknameAndChannelOrderByIdAsc(nickname, "WECHAT")
                            .orElseGet(
                                    () -> {
                                        WechatUser u = new WechatUser();
                                        u.setChannel("WECHAT");
                                        u.setWechatNickname(nickname);
                                        Instant n = Instant.now();
                                        u.setCreatedAt(n);
                                        u.setUpdatedAt(n);
                                        u.setArchived(false);
                                        return wechatUserRepo.save(u);
                                    });
        } else {
            String nick = "账单用户-" + PhoneUtil.normalizeCnMobile(mobileCn);
            wu =
                    wechatUserRepo
                            .findFirstByWechatNicknameAndChannelOrderByIdAsc(nick, "ALIPAY")
                            .orElseGet(
                                    () -> {
                                        WechatUser u = new WechatUser();
                                        u.setChannel("ALIPAY");
                                        u.setWechatNickname(nick);
                                        Instant n = Instant.now();
                                        u.setCreatedAt(n);
                                        u.setUpdatedAt(n);
                                        u.setArchived(false);
                                        return wechatUserRepo.save(u);
                                    });
        }

        // 建立 person / phone 关联链路
        billImportLinkageService.ensurePhoneAndPersonLinked(wu, mobileCn);
        wu = wechatUserRepo.findById(wu.getId()).orElse(wu);
        Long personId = wu.getPersonId();
        Long phoneId = wu.getPhoneId();

        WechatBillImport imp = new WechatBillImport();
        imp.setChannel(channel);
        imp.setUserId(wu.getId());
        imp.setPersonId(personId);
        imp.setPhoneId(phoneId);
        imp.setMobileCn(PhoneUtil.normalizeCnMobile(mobileCn));
        imp.setSourceFile(Objects.requireNonNullElse(file.getOriginalFilename(), "upload.csv"));
        applyMetaToImport(imp, meta);

        Instant now = Instant.now();
        imp.setCreatedAt(now);
        imp.setUpdatedAt(now);
        imp.setArchived(false);
        imp.setCreatedBy("spring-import");
        imp.setUpdatedBy("spring-import");
        imp = importRepo.save(imp);

        for (String[] vals : dataRows) {
            WechatBillTransaction t = new WechatBillTransaction();
            t.setChannel(channel);
            t.setBillImportId(imp.getId());
            if (isWechat) {
                t.setTradeTime(getAny(col, vals, "交易时间", "时间", "交易创建时间"));
                t.setTradeType(get(col, vals, "交易类型"));
                t.setCounterparty(get(col, vals, "交易对方"));
                t.setProduct(get(col, vals, "商品"));
                t.setIncomeExpense(getAny(col, vals, "收/支", "收支"));
                t.setAmountYuan(parseAmt(get(col, vals, "金额(元)")));
                t.setPaymentMethod(get(col, vals, "支付方式"));
                t.setStatus(get(col, vals, "当前状态"));
                t.setTradeNo(get(col, vals, "交易单号"));
                t.setMerchantNo(get(col, vals, "商户单号"));
                t.setRemark(get(col, vals, "备注"));
            } else {
                t.setTradeTime(getAny(col, vals, "交易时间", "时间", "交易创建时间"));
                t.setTradeType(getAny(col, vals, "交易分类", "交易类型"));
                t.setCounterparty(getAny(col, vals, "交易对方", "对方名称"));
                t.setProduct(getAny(col, vals, "商品说明", "商品名称", "商品"));
                t.setIncomeExpense(getAny(col, vals, "收/支", "收支"));
                t.setAmountYuan(parseAmt(getAny(col, vals, "金额", "金额(元)")));
                t.setPaymentMethod(getAny(col, vals, "收/付款方式", "支付方式"));
                t.setStatus(getAny(col, vals, "交易状态", "当前状态"));
                t.setTradeNo(getAny(col, vals, "交易订单号", "交易单号"));
                t.setMerchantNo(getAny(col, vals, "商家订单号", "商户订单号"));
                t.setRemark(getAny(col, vals, "备注", "备注信息"));
            }
            t.setSourceFile(imp.getSourceFile());
            t.setPersonId(personId);
            t.setPhoneId(phoneId);
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
        backfillImportStatsFromRows(imp.getId(), channel, dataRows, col);
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
                existing = txMapper.findByMobileCnAndRowHash(mobile.trim(), hash);
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
        existing.setChannel(t.getChannel());
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

    private String getAny(Map<String, Integer> col, String[] vals, String... names) {
        for (String n : names) {
            if (n == null || n.isBlank()) {
                continue;
            }
            String v = get(col, vals, normalizeHeader(n));
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }

    private static String normalizeHeader(String h) {
        if (h == null) {
            return "";
        }
        String s = h.trim();
        // 处理 BOM（常见于 CSV 第一列表头）
        s = s.replace("\uFEFF", "");
        // 去掉首尾引号
        s = s.replaceAll("^\"|\"$", "");
        return s.trim();
    }

    /**
     * 行尾逗号、多余分隔符会多出一列空表头；Commons CSV 的 {@code setHeader} 要求每个表头名非空、互不相同。
     */
    private static String[] ensureNonBlankUniqueCsvHeaders(String[] names) {
        if (names == null) {
            return new String[0];
        }
        String[] out = new String[names.length];
        Set<String> used = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            String h = names[i] == null ? "" : names[i].trim();
            if (h.isEmpty()) {
                h = "_unnamed_" + i;
            }
            String candidate = h;
            int d = 0;
            while (used.contains(candidate)) {
                d++;
                candidate = h + "__" + d;
            }
            used.add(candidate);
            out[i] = candidate;
        }
        return out;
    }

    private void backfillImportStatsFromRows(
            Long importId, String channel, List<String[]> rows, Map<String, Integer> col) {
        if (importId == null) {
            return;
        }
        WechatBillImport imp = importRepo.findById(importId).orElse(null);
        if (imp == null) {
            return;
        }
        // 若已存在统计且非 0，则不覆盖
        boolean hasAny =
                (imp.getTotalCount() != null && imp.getTotalCount() > 0)
                        || (imp.getIncomeCount() != null && imp.getIncomeCount() > 0)
                        || (imp.getExpenseCount() != null && imp.getExpenseCount() > 0)
                        || (imp.getNeutralCount() != null && imp.getNeutralCount() > 0);
        if (hasAny) {
            return;
        }
        int total = 0;
        int ic = 0;
        int ec = 0;
        int nc = 0;
        BigDecimal ia = BigDecimal.ZERO;
        BigDecimal ea = BigDecimal.ZERO;
        BigDecimal na = BigDecimal.ZERO;
        for (String[] vals : rows) {
            total++;
            String ie = getAny(col, vals, "收/支", "收支");
            BigDecimal amt = parseAmt(getAny(col, vals, "金额", "金额(元)"));
            if (amt == null) {
                amt = BigDecimal.ZERO;
            }
            // 兼容：支付宝常见值：支出/收入/不计收支/中性交易
            String tag = (ie == null ? "" : ie.trim());
            if (tag.contains("支出") || tag.equalsIgnoreCase("expense")) {
                ec++;
                ea = ea.add(amt.abs());
            } else if (tag.contains("收入") || tag.equalsIgnoreCase("income")) {
                ic++;
                ia = ia.add(amt.abs());
            } else {
                nc++;
                na = na.add(amt.abs());
            }
        }
        imp.setChannel(channel);
        imp.setTotalCount(total);
        imp.setIncomeCount(ic);
        imp.setIncomeAmount(ia);
        imp.setExpenseCount(ec);
        imp.setExpenseAmount(ea);
        imp.setNeutralCount(nc);
        imp.setNeutralAmount(na);
        importRepo.save(imp);
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

    /** 微信 CSV 表头：含 交易单号、金额(元)，与支付宝「交易订单号」区分。 */
    private static boolean isWechatCsvHeader(String line) {
        if (line == null) {
            return false;
        }
        return line.contains("交易时间")
                && line.contains("交易单号")
                && (line.contains("金额(元)") || line.contains("金额"));
    }

    /** 支付宝 CSV 表头。 */
    private static boolean isAlipayCsvHeader(String line) {
        if (line == null) {
            return false;
        }
        return line.contains("交易时间")
                && (line.contains("交易订单号") || line.contains("商家订单号"));
    }

    private static char detectCsvDelimiter(String line) {
        if (line == null) {
            return ',';
        }
        if (line.indexOf('\t') >= 0 && line.indexOf(',') < 0) {
            return '\t';
        }
        return ',';
    }

    /** 单行 RFC4180 解析，用于表头列名。 */
    private String[] parseOneCsvLine(String line, char delimiter) throws IOException {
        if (line == null) {
            return new String[0];
        }
        CSVFormat fmt =
                CSVFormat.Builder.create(CSVFormat.RFC4180).setDelimiter(delimiter).build();
        try (CSVParser p = fmt.parse(new StringReader(line + "\n"))) {
            for (CSVRecord r : p) {
                int n = r.size();
                String[] a = new String[n];
                for (int i = 0; i < n; i++) {
                    a[i] = r.get(i) == null ? "" : r.get(i);
                }
                return a;
            }
        }
        return new String[0];
    }

    /** 将meta信息应用到import记录 */
    private void applyMetaToImport(WechatBillImport imp, Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) return;
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
    }
}
