package org.example.bill.util;

/**
 * 账单导出里「收/支」列：收入、支出、中性（含部分导出为 {@code /} 的中性交易）。
 */
public final class BillIncomeExpenseUtil {

    private BillIncomeExpenseUtil() {}

    public static boolean isIncome(String ie) {
        return ie != null && ie.contains("收入");
    }

    public static boolean isExpense(String ie) {
        return ie != null && ie.contains("支出");
    }

    /** 中性：含「中性」字样，或部分平台导出为单独的 {@code /}。 */
    public static boolean isNeutral(String ie) {
        if (ie == null) {
            return false;
        }
        String t = ie.trim();
        if (t.isEmpty()) {
            return false;
        }
        if ("/".equals(t) || "／".equals(t)) {
            return true;
        }
        return ie.contains("中性");
    }
}
