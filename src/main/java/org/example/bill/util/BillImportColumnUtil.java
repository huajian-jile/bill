package org.example.bill.util;

/** 账单文件表头：去 BOM、全角空格等，便于与导出列名对齐。 */
public final class BillImportColumnUtil {

    private BillImportColumnUtil() {}

    public static String normalizeHeader(String h) {
        if (h == null) {
            return "";
        }
        return h.replace("\uFEFF", "")
                .replace('\u00A0', ' ')
                .replace('\u3000', ' ')
                .trim();
    }
}
