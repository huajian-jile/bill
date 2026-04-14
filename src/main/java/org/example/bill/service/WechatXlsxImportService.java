package org.example.bill.service;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
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
import org.example.bill.domain.WechatBillImport;
import org.example.bill.domain.WechatBillTransaction;
import org.example.bill.domain.WechatUser;
import org.example.bill.repo.WechatBillImportRepository;
import org.example.bill.repo.WechatBillTransactionRepository;
import org.example.bill.repo.WechatUserRepository;
import org.example.bill.util.PhoneUtil;
import org.example.bill.util.RowHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class WechatXlsxImportService {

    public static final String CHANNEL_WECHAT = "WECHAT";
    public static final String CHANNEL_ALIPAY = "ALIPAY";

    /** 昵称 */
    private static final Pattern NICK = Pattern.compile(".*(?:微信)?昵称[：:]\\s*\\[([^]]+)]");
    /** 起始时间：微信「起始时间」 / 支付宝「开始时间」 */
    private static final Pattern RANGE_START = Pattern.compile(".*(?:起始|开始)(?:时间)?\\s*[：:]?\\s*(.+)");
    /** 终止时间 */
    private static final Pattern RANGE_END = Pattern.compile(".*终止(?:时间)?\\s*[：:]?\\s*(.+)");
    /** 导出时间 */
    private static final Pattern EXPORT_TIME = Pattern.compile(".*导出(?:时间)?\\s*[：:]?\\s*(.+)");
    /** 导出类型：微信「导出类型：[全部]」 / 支付宝「导出交易类型：[全部]」 */
    private static final Pattern EXPORT_TYPE = Pattern.compile(".*导出交易?类?型?\\s*[：:]?\\s*(.+)");
    /** 共 30 笔 */
    private static final Pattern TOTAL = Pattern.compile(".*共\\s*(\\d+)\\s*笔");
    /** 收入 10 笔 1.00 元 */
    private static final Pattern INCOME = Pattern.compile(".*收?入\\s*[：:]?\\s*(\\d+)\\s*笔\\s*([\\d.]+)\\s*元?");
    /** 支出 20 笔 2.00 元 */
    private static final Pattern EXPENSE = Pattern.compile(".*支?出\\s*[：:]?\\s*(\\d+)\\s*笔\\s*([\\d.]+)\\s*元?");
    /** 微信「中性交易：56笔，2000.0元」 / 支付宝「不计收支：xxx笔，xxx元」 */
    private static final Pattern NEUTRAL = Pattern.compile(".*(?:中性交易|不计收支)\\s*[：:]?\\s*(\\d+)\\s*笔[，,]\\s*([\\d.]+)\\s*元");

    private final WechatUserRepository wechatUserRepo;
    private final WechatBillImportRepository importRepo;
    private final WechatBillTransactionRepository txRepo;
    private final BillImportLinkageService billImportLinkageService;

    /** 微信账单（仅 xlsx） */
    @Transactional
    public WechatBillImport importWechat(MultipartFile file, String mobileCn) throws Exception {
        PhoneUtil.requireValidCnMobile(mobileCn);
        String fn = file.getOriginalFilename();
        if (fn != null && fn.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("微信账单仅支持 xlsx 格式，不支持 CSV");
        }
        return importBill(file, mobileCn, CHANNEL_WECHAT);
    }

    /** 支付宝账单（仅 CSV） */
    @Transactional
    public WechatBillImport importAlipay(MultipartFile file, String mobileCn) throws Exception {
        PhoneUtil.requireValidCnMobile(mobileCn);
        String fn = file.getOriginalFilename();
        if (fn != null && fn.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("支付宝账单仅支持 CSV 格式，不支持 xlsx");
        }
        return importBill(file, mobileCn, CHANNEL_ALIPAY);
    }

    @Transactional
    public WechatBillImport importBill(MultipartFile file, String mobileCn, String channel) throws Exception {
        String fn = file.getOriginalFilename();
        if (fn != null && fn.toLowerCase().endsWith(".csv")) {
            return importCsv(file, mobileCn, channel);
        }
        return importXlsx(file, mobileCn, channel);
    }

    /**
     * xlsx 解析：遍历所有行，提取 meta 信息（昵称/时间范围/统计），表头行之后是数据。
     * 微信和支付宝格式相同，共用解析逻辑。
     */
    @Transactional
    public WechatBillImport importXlsx(MultipartFile file, String mobileCn, String channel) throws Exception {
        Map<String, Object> meta = new LinkedHashMap<>();
        List<String[]> dataRows = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        try (InputStream in = file.getInputStream();
                Workbook wb = new XSSFWorkbook(in)) {
            Sheet sh = wb.getSheetAt(0);
            boolean headerFound = false;
            for (Row row : sh) {
                String line = rowText(row);
                if (!headerFound) {
                    parseMetaLine(line, meta, channel);
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
                if (isEmptyRow(row)) continue;
                String[] vals = new String[headers.size()];
                Arrays.fill(vals, "");
                for (int i = 0; i < headers.size(); i++) {
                    vals[i] = cellStr(row.getCell(i)).trim();
                }
                dataRows.add(vals);
            }
        }

        if (headers.isEmpty()) {
            throw new IllegalArgumentException("未找到账单表头行");
        }

        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (!headers.get(i).isBlank()) col.put(headers.get(i), i);
        }

        String nickname = Optional.ofNullable(meta.get("nickname"))
                .map(Object::toString).orElse("未知用户");

        // DEBUG：打印解析到的 meta 信息
        System.out.println("[DEBUG] parseMeta result: " + meta);
        System.out.println("[DEBUG] headers: " + headers);

        return doImport(dataRows, col, nickname, mobileCn, channel, file, meta);
    }

    /** CSV 解析：支付宝 CSV 通常是 GBK 编码。 */
    @Transactional
    public WechatBillImport importCsv(MultipartFile file, String mobileCn, String channel) throws Exception {
        List<String[]> dataRows = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        Map<String, Object> meta = new LinkedHashMap<>();

        Charset gbk = Charset.forName("GBK");
        Charset csvCharset = detectCsvCharset(file, gbk);
        System.out.println("[DEBUG] CSV charset: " + csvCharset);
        try (Reader reader = new InputStreamReader(file.getInputStream(), csvCharset)) {
            CSVFormat fmt = CSVFormat.DEFAULT.builder().build();
            try (CSVParser parser = fmt.parse(reader)) {
                Iterator<CSVRecord> it = parser.iterator();
                boolean headerFound = false;
                while (it.hasNext()) {
                    CSVRecord rec = it.next();
                    if (!headerFound) {
                        // 把整行拼成字符串用于 parseMetaLine
                        StringBuilder lineSb = new StringBuilder();
                        for (String v : rec) {
                            if (v != null) lineSb.append(v.trim()).append(" ");
                        }
                        String line = lineSb.toString().trim();
                        // 解析 meta（与 xlsx 共用同一方法）
                        parseMetaLine(line, meta, channel);
                        // 检查是否是表头行
                        boolean isHeader = csvLooksLikeHeader(rec);
                        System.out.println("[DEBUG] CSV row check: isHeader=" + isHeader + " cells=" + rec.toList());
                        if (isHeader) {
                            for (String h : rec) {
                                headers.add(h != null ? h.trim() : "");
                            }
                            headerFound = true;
                        }
                        continue;
                    }
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

        if (headers.isEmpty()) throw new IllegalArgumentException("CSV 无表头");
        System.out.println("[DEBUG] CSV headers resolved: " + headers);
        System.out.println("[DEBUG] CSV meta resolved: " + meta);
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            if (!headers.get(i).isBlank()) col.put(headers.get(i).trim(), i);
        }
        System.out.println("[DEBUG] CSV col map: " + col);
        if (!col.containsKey("交易时间")) {
            throw new IllegalArgumentException("CSV 表头缺少「交易时间」列，实际表头：" + headers);
        }

        String normalizedMobile = PhoneUtil.normalizeCnMobile(mobileCn);
        String nickname = Optional.ofNullable(meta.get("nickname"))
                .map(Object::toString).orElse("账单用户-" + normalizedMobile);
        return doImport(dataRows, col, nickname, mobileCn, channel, file, meta);
    }

    /** 判断 CSV 的一行是否是表头行：包含「交易时间」且含「单号/金额/收/支/状态」等关键字 */
    private boolean csvLooksLikeHeader(CSVRecord rec) {
        Set<String> cells = new HashSet<>();
        for (String v : rec) {
            if (v != null && !v.trim().isEmpty()) cells.add(v.trim());
        }
        System.out.println("[DEBUG] csvLooksLikeHeader check, cells=" + cells);
        boolean hasTime = cells.stream().anyMatch(c -> c.contains("交易时间") || c.contains("时间"));
        boolean hasKey = cells.stream().anyMatch(c ->
                c.contains("单号") || c.contains("金额") || c.contains("收/支") || c.contains("收支")
                        || c.contains("状态") || c.contains("支付方式") || c.contains("交易对方")
                        || c.contains("商品") || c.contains("资金状态") || c.contains("对方"));
        return hasTime && hasKey;
    }

    private Charset detectCsvCharset(MultipartFile file, Charset defaultCharset) throws Exception {
        byte[] head = file.getInputStream().readNBytes(4096);
        String s = new String(head, defaultCharset);
        // 若用 GBK 读出来全是乱码（无中文无字母），换 UTF-8
        if (isGarbled(s)) {
            return java.nio.charset.StandardCharsets.UTF_8;
        }
        return defaultCharset;
    }

    private boolean isGarbled(String s) {
        int letters = 0, chinese = 0;
        for (char c : s.toCharArray()) {
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) letters++;
            else if (c >= 0x4E00 && c <= 0x9FFF) chinese++;
        }
        return letters == 0 && chinese == 0;
    }


    private WechatBillImport doImport(
            List<String[]> dataRows,
            Map<String, Integer> col,
            String nickname,
            String mobileCn,
            String channel,
            MultipartFile file,
            Map<String, Object> meta) {

        String normalizedMobile = PhoneUtil.normalizeCnMobile(mobileCn);

        // 1. 查找或创建用户（channel + nickname 唯一）
        WechatUser wu = wechatUserRepo
                .findFirstByWechatNicknameAndChannelOrderByIdAsc(nickname, channel)
                .orElseGet(() -> {
                    WechatUser u = new WechatUser();
                    u.setChannel(channel);
                    u.setWechatNickname(nickname);
                    Instant n = Instant.now();
                    u.setCreatedAt(n);
                    u.setUpdatedAt(n);
                    u.setArchived(false);
                    return wechatUserRepo.save(u);
                });

        // 2. 建立 person / phone 关联链路
        billImportLinkageService.ensurePhoneAndPersonLinked(wu, mobileCn);
        wu = wechatUserRepo.findById(wu.getId()).orElse(wu);
        Long personId = wu.getPersonId();
        Long phoneId = wu.getPhoneId();

        // 3. 创建 wechat_bill_imports（共用表，channel 区分）
        WechatBillImport imp = new WechatBillImport();
        imp.setChannel(channel);
        imp.setUserId(wu.getId());
        imp.setPersonId(personId);
        imp.setPhoneId(phoneId);
        imp.setMobileCn(normalizedMobile);
        imp.setSourceFile(Objects.requireNonNullElse(file.getOriginalFilename(), "upload"));

        // meta 信息（xlsx 有，CSV 为空）
        applyMetaToImport(imp, meta);

        Instant now = Instant.now();
        imp.setCreatedAt(now);
        imp.setUpdatedAt(now);
        imp.setArchived(false);
        imp.setCreatedBy("spring-import");
        imp.setUpdatedBy("spring-import");
        imp = importRepo.save(imp);

        // 4. 解析并持久化每条交易明细
        for (String[] vals : dataRows) {
            if (!isDataRow(col, vals)) continue;
            WechatBillTransaction t = makeTransaction(col, vals, channel, imp, personId, phoneId, normalizedMobile, now);
            persistTransaction(t);
        }
        return imp;
    }

    /** 简单判断是否是有效数据行（至少有一列非空）*/
    private boolean isDataRow(Map<String, Integer> col, String[] vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return true;
        }
        return false;
    }

    private WechatBillTransaction makeTransaction(
            Map<String, Integer> col,
            String[] vals,
            String channel,
            WechatBillImport imp,
            Long personId,
            Long phoneId,
            String normalizedMobile,
            Instant now) {
        WechatBillTransaction t = new WechatBillTransaction();
        t.setChannel(channel);
        t.setBillImportId(imp.getId());

        // 宽松列名匹配：遍历 col 的 key，用 contains 找最接近的列
        t.setTradeTime(     looseGet(col, vals, "交易时间", "时间"));
        t.setTradeType(     looseGet(col, vals, "交易类型", "类型", "交易"));
        t.setCounterparty(  looseGet(col, vals, "交易对方", "对方", "交易对方"));
        t.setProduct(       looseGet(col, vals, "商品", "商品说明", "商品名称", "物品"));
        t.setIncomeExpense( looseGet(col, vals, "收/支", "收支", "资金状态", "收付款类型"));
        t.setAmountYuan(    parseAmt(looseGet(col, vals, "金额(元)", "金额（元）", "金额", "金额(元)")));
        t.setPaymentMethod( looseGet(col, vals, "支付方式", "收付款方式", "付款方式", "支付渠道"));
        t.setStatus(       looseGet(col, vals, "当前状态", "交易状态", "状态", "资金状态"));
        t.setTradeNo(       looseGet(col, vals, "交易单号", "交易订单号", "订单号"));
        t.setMerchantNo(    looseGet(col, vals, "商户单号", "商家订单号", "商家单号", "商户订单号"));
        t.setRemark(        looseGet(col, vals, "备注", "备注信息", "交易备注", "说明"));

        t.setSourceFile(imp.getSourceFile());
        t.setPersonId(personId);
        t.setPhoneId(phoneId);
        t.setMobileCn(normalizedMobile);
        t.setRowHash(RowHashUtil.hash(t.getTradeTime(), t.getTradeNo(), t.getMerchantNo(),
                t.getAmountYuan(), t.getTradeType(), t.getCounterparty()));
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        t.setCreatedBy("spring-import");
        t.setUpdatedBy("spring-import");
        t.setArchived(false);
        return t;
    }

    /** 按 row_hash 查找；未命中且有 trade_no 则按 trade_no 查找；都未命中则插入。 */
    private void persistTransaction(WechatBillTransaction t) {
        String hash = t.getRowHash();
        String tradeNo = t.getTradeNo();

        Optional<WechatBillTransaction> existing = null;

        // 1. 先按 row_hash 查（person_id + row_hash 唯一）
        if (hash != null && !hash.isBlank()) {
            existing = txRepo.findByPersonIdAndRowHash(t.getPersonId(), hash);
        }

        // 2. row_hash 未命中，但 trade_no 重复（唯一约束冲突），按 trade_no 更新
        if (existing == null || existing.isEmpty()) {
            if (tradeNo != null && !tradeNo.isBlank()) {
                existing = txRepo.findByTradeNo(tradeNo);
            }
        }

        if (existing != null && existing.isPresent()) {
            applyRowToExisting(existing.get(), t);
            txRepo.save(existing.get());
        } else {
            txRepo.save(t);
        }
    }

    private void applyRowToExisting(WechatBillTransaction existing, WechatBillTransaction t) {
        existing.setChannel(t.getChannel()); // channel 可能变化（如旧数据）
        existing.setBillImportId(t.getBillImportId());
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
        existing.setUpdatedAt(Instant.now());
        existing.setUpdatedBy("spring-import");
    }

    private void applyMetaToImport(WechatBillImport imp, Map<String, Object> meta) {
        if (meta.isEmpty()) return;
        imp.setExportType(str(meta.get("export_type")));
        imp.setExportTime(parseInstant(meta.get("export_time")));
        imp.setRangeStart(parseInstant(meta.get("range_start")));
        imp.setRangeEnd(parseInstant(meta.get("range_end")));
        if (meta.get("total_count") != null)
            imp.setTotalCount(((Number) meta.get("total_count")).intValue());
        if (meta.get("income_count") != null)
            imp.setIncomeCount(((Number) meta.get("income_count")).intValue());
        if (meta.get("income_amount") != null)
            imp.setIncomeAmount(new BigDecimal(meta.get("income_amount").toString()));
        if (meta.get("expense_count") != null)
            imp.setExpenseCount(((Number) meta.get("expense_count")).intValue());
        if (meta.get("expense_amount") != null)
            imp.setExpenseAmount(new BigDecimal(meta.get("expense_amount").toString()));
        if (meta.get("neutral_count") != null)
            imp.setNeutralCount(((Number) meta.get("neutral_count")).intValue());
        if (meta.get("neutral_amount") != null)
            imp.setNeutralAmount(new BigDecimal(meta.get("neutral_amount").toString()));
    }

    /** 从捕获值中去掉首尾的方括号和空格 */
    private static String stripBrackets(String s) {
        if (s == null) return "";
        s = s.trim();
        while (s.startsWith("[") || s.startsWith("【") || s.startsWith("〔")) s = s.substring(1);
        while (s.endsWith("]") || s.endsWith("】") || s.endsWith("〕")) s = s.substring(0, s.length() - 1);
        return s.trim();
    }

    /**
     * 解析 meta 行（昵称/时间范围/统计）。channel 仅用于日志区分，匹配逻辑已统一宽松化。
     */
    void parseMetaLine(String line, Map<String, Object> meta, String channel) {
        if (line == null || line.isBlank()) return;
        System.out.println("[DEBUG] parseMetaLine channel=" + channel + " line=[" + line + "]");
        Matcher m;

        // 昵称（宽松）
        m = NICK.matcher(line);
        if (m.find()) {
            System.out.println("[DEBUG] NICK matched: " + m.group(1));
            meta.put("nickname", m.group(1).trim());
        }

        // 起始时间（独立一行，格式：起始时间：[2026-04-13 14:23:06]）
        m = RANGE_START.matcher(line);
        if (m.find()) {
            String start = stripBrackets(m.group(1));
            System.out.println("[DEBUG] RANGE_START matched: '" + start + "'");
            meta.put("range_start", start);
        } else {
            System.out.println("[DEBUG] RANGE_START NOT matched for: '" + line + "'");
        }

        // 终止时间（独立一行）
        m = RANGE_END.matcher(line);
        if (m.find()) {
            String end = stripBrackets(m.group(1));
            System.out.println("[DEBUG] RANGE_END matched: '" + end + "'");
            meta.put("range_end", end);
        } else {
            System.out.println("[DEBUG] RANGE_END NOT matched for: '" + line + "'");
        }

        // 导出时间
        m = EXPORT_TIME.matcher(line);
        if (m.find()) {
            String t = stripBrackets(m.group(1));
            System.out.println("[DEBUG] EXPORT_TIME matched: '" + t + "'");
            meta.put("export_time", t);
        }

        // 导出类型
        m = EXPORT_TYPE.matcher(line);
        if (m.find()) {
            String t = stripBrackets(m.group(1));
            System.out.println("[DEBUG] EXPORT_TYPE matched: '" + t + "'");
            meta.put("export_type", t);
        } else {
            System.out.println("[DEBUG] EXPORT_TYPE NOT matched for: '" + line + "'");
        }

        // 总笔数
        m = TOTAL.matcher(line);
        if (m.find()) {
            System.out.println("[DEBUG] TOTAL matched: " + m.group(1));
            meta.put("total_count", Integer.parseInt(m.group(1)));
        }

        // 收入
        m = INCOME.matcher(line);
        if (m.find()) {
            System.out.println("[DEBUG] INCOME matched: " + m.group(1) + " 笔 " + m.group(2) + " 元");
            meta.put("income_count", Integer.parseInt(m.group(1)));
            meta.put("income_amount", new BigDecimal(m.group(2)));
        }

        // 支出
        m = EXPENSE.matcher(line);
        if (m.find()) {
            System.out.println("[DEBUG] EXPENSE matched: " + m.group(1) + " 笔 " + m.group(2) + " 元");
            meta.put("expense_count", Integer.parseInt(m.group(1)));
            meta.put("expense_amount", new BigDecimal(m.group(2)));
        }

        // 中性：微信「中性交易：56笔，2000.0元」 / 支付宝「不计收支：xxx笔，xxx元」
        m = NEUTRAL.matcher(line);
        if (m.find()) {
            System.out.println("[DEBUG] NEUTRAL matched: " + m.group(1) + " 笔 " + m.group(2) + " 元");
            meta.put("neutral_count", Integer.parseInt(m.group(1)));
            meta.put("neutral_amount", new BigDecimal(m.group(2)));
        } else {
            System.out.println("[DEBUG] NEUTRAL NOT matched for: '" + line + "'");
        }
    }

    private boolean looksLikeHeader(Row row) {
        if (row == null) return false;
        Set<String> cellTexts = new HashSet<>();
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null) cellTexts.add(cellStr(cell).trim());
        }
        System.out.println("[DEBUG] looksLikeHeader check, cellTexts=" + cellTexts);
        // 用 contains 判断「交易时间」是否存在（允许变体如「交易时间(北京)」）
        boolean hasTime = cellTexts.stream().anyMatch(t -> t.contains("交易时间") || t.contains("时间"));
        // 同时有任一关键列
        boolean hasKey = cellTexts.stream().anyMatch(t ->
                t.contains("单号") || t.contains("金额") || t.contains("收/支") || t.contains("状态")
                        || t.contains("收支") || t.contains("交易") || t.contains("商品")
                        || t.contains("对方") || t.contains("方式"));
        return hasTime && hasKey;
    }

    private String rowText(Row row) {
        StringBuilder sb = new StringBuilder();
        if (row == null) return "";
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell == null) continue;
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                sb.append(LocalDateTime.ofInstant(cell.getDateCellValue().toInstant(),
                        ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } else {
                sb.append(cellStr(cell));
            }
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String cellStr(Cell c) {
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(c)
                    ? LocalDateTime.ofInstant(c.getDateCellValue().toInstant(), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : BigDecimal.valueOf(c.getNumericCellValue()).stripTrailingZeros().toPlainString();
            case BOOLEAN -> Boolean.toString(c.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && !cellStr(cell).isBlank()) return false;
        }
        return true;
    }

    private BigDecimal parseAmt(String s) {
        if (s == null || s.isBlank() || "/".equals(s)) return null;
        try {
            return new BigDecimal(s.replace(",", "").replace("¥", "").replace("元", ""));
        } catch (Exception e) { return null; }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        return b == null ? "" : b;
    }

    private static String firstNonBlank(String a, String b, String c) {
        return firstNonBlank(firstNonBlank(a, b), c);
    }

    private static String firstNonBlank(String a, String b, String c, String d) {
        return firstNonBlank(firstNonBlank(a, b, c), d);
    }

    private static String str(Object o) { return o == null ? null : o.toString(); }

    private static String get(Map<String, Integer> col, String[] vals, String name) {
        Integer i = col.get(name);
        if (i == null || i >= vals.length) return "";
        return vals[i] == null ? "" : vals[i];
    }

    /**
     * 宽松列名查找：先用精确 key，再用 contains 匹配。
     * col 的 key 是原始表头名称，尝试每个 candidate，找到第一个存在的列索引。
     */
    private String looseGet(Map<String, Integer> col, String[] vals, String... candidates) {
        for (String cand : candidates) {
            // 精确匹配
            if (col.containsKey(cand)) {
                String v = get(col, vals, cand);
                if (!v.isBlank()) return v;
            }
            // contains 匹配（遍历 col 的 key）
            for (Map.Entry<String, Integer> e : col.entrySet()) {
                if (e.getKey().contains(cand) || cand.contains(e.getKey())) {
                    String v = get(col, vals, e.getKey());
                    if (!v.isBlank()) return v;
                }
            }
        }
        return "";
    }

    private Instant parseInstant(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        // 替换中文年月日分隔符
        s = s.replace("年", "-").replace("月", "-").replace("日", "");
        // 斜杠变横杠
        s = s.replace("/", "-");
        List<String> fmts = List.of(
                "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm", "yyyy/MM/dd HH:mm:ss",
                "yyyy/MM/dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd");
        for (String fmt : fmts) {
            try {
                var fmtter = DateTimeFormatter.ofPattern(fmt);
                if (fmt.contains("HH")) {
                    return LocalDateTime.parse(s, fmtter).atZone(ZoneId.systemDefault()).toInstant();
                }
                return java.time.LocalDate.parse(s, fmtter).atStartOfDay(ZoneId.systemDefault()).toInstant();
            } catch (Exception ignored) {}
        }
        return null;
    }
}
