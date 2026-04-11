package org.example.bill.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.example.bill.domain.AlipayBillTransaction;
import org.example.bill.domain.WechatBillTransaction;
import org.example.bill.util.TradeTimeUtil;

/**
 * 识别「同一天、金额相同、一收一支」的成对交易（典型：微信转账与退回），用于从真实收支中剔除。
 */
public final class RefundPairFinder {

    private RefundPairFinder() {}

    public static Set<Long> findPairedTransactionIds(List<WechatBillTransaction> txs) {
        Map<LocalDate, Map<String, List<WechatBillTransaction>>> byDayAmount = new HashMap<>();
        for (WechatBillTransaction t : txs) {
            var d = TradeTimeUtil.parseDate(t.getTradeTime());
            if (d.isEmpty()) {
                continue;
            }
            if (t.getAmountYuan() == null) {
                continue;
            }
            String amtKey = t.getAmountYuan().stripTrailingZeros().toPlainString();
            byDayAmount
                    .computeIfAbsent(d.get(), x -> new HashMap<>())
                    .computeIfAbsent(amtKey, x -> new ArrayList<>())
                    .add(t);
        }
        Set<Long> excluded = new HashSet<>();
        for (var dayEntry : byDayAmount.entrySet()) {
            for (var amtEntry : dayEntry.getValue().entrySet()) {
                List<WechatBillTransaction> list = amtEntry.getValue();
                if (list.size() < 2) {
                    continue;
                }
                List<WechatBillTransaction> incomes = new ArrayList<>();
                List<WechatBillTransaction> expenses = new ArrayList<>();
                for (WechatBillTransaction t : list) {
                    String ie = t.getIncomeExpense();
                    if (ie == null) {
                        continue;
                    }
                    if (ie.contains("收入")) {
                        incomes.add(t);
                    } else if (ie.contains("支出")) {
                        expenses.add(t);
                    }
                }
                int pairs = Math.min(incomes.size(), expenses.size());
                for (int i = 0; i < pairs; i++) {
                    excluded.add(incomes.get(i).getId());
                    excluded.add(expenses.get(i).getId());
                }
            }
        }
        return excluded;
    }

    /** 支付宝账单：同日同金额一收一支（逻辑与微信一致） */
    public static Set<Long> findPairedAlipayTransactionIds(List<AlipayBillTransaction> txs) {
        Map<LocalDate, Map<String, List<AlipayBillTransaction>>> byDayAmount = new HashMap<>();
        for (AlipayBillTransaction t : txs) {
            var d = TradeTimeUtil.parseDate(t.getTradeTime());
            if (d.isEmpty()) {
                continue;
            }
            if (t.getAmountYuan() == null) {
                continue;
            }
            String amtKey = t.getAmountYuan().stripTrailingZeros().toPlainString();
            byDayAmount
                    .computeIfAbsent(d.get(), x -> new HashMap<>())
                    .computeIfAbsent(amtKey, x -> new ArrayList<>())
                    .add(t);
        }
        Set<Long> excluded = new HashSet<>();
        for (var dayEntry : byDayAmount.entrySet()) {
            for (var amtEntry : dayEntry.getValue().entrySet()) {
                List<AlipayBillTransaction> list = amtEntry.getValue();
                if (list.size() < 2) {
                    continue;
                }
                List<AlipayBillTransaction> incomes = new ArrayList<>();
                List<AlipayBillTransaction> expenses = new ArrayList<>();
                for (AlipayBillTransaction t : list) {
                    String ie = t.getIncomeExpense();
                    if (ie == null) {
                        continue;
                    }
                    if (ie.contains("收入")) {
                        incomes.add(t);
                    } else if (ie.contains("支出")) {
                        expenses.add(t);
                    }
                }
                int pairs = Math.min(incomes.size(), expenses.size());
                for (int i = 0; i < pairs; i++) {
                    excluded.add(incomes.get(i).getId());
                    excluded.add(expenses.get(i).getId());
                }
            }
        }
        return excluded;
    }

    public static BigDecimal sumAmount(
            List<WechatBillTransaction> txs, Set<Long> excluded, IncomeExpenseFilter filter) {
        BigDecimal s = BigDecimal.ZERO;
        for (WechatBillTransaction t : txs) {
            if (excluded.contains(t.getId())) {
                continue;
            }
            if (t.getAmountYuan() == null) {
                continue;
            }
            String ie = t.getIncomeExpense();
            if (!filter.matches(ie)) {
                continue;
            }
            s = s.add(t.getAmountYuan());
        }
        return s;
    }

    public static BigDecimal sumAlipayAmount(
            List<AlipayBillTransaction> txs, Set<Long> excluded, IncomeExpenseFilter filter) {
        BigDecimal s = BigDecimal.ZERO;
        for (AlipayBillTransaction t : txs) {
            if (excluded.contains(t.getId())) {
                continue;
            }
            if (t.getAmountYuan() == null) {
                continue;
            }
            String ie = t.getIncomeExpense();
            if (!filter.matches(ie)) {
                continue;
            }
            s = s.add(t.getAmountYuan());
        }
        return s;
    }

    public enum IncomeExpenseFilter {
        INCOME {
            @Override
            boolean matches(String ie) {
                return ie != null && ie.contains("收入");
            }
        },
        EXPENSE {
            @Override
            boolean matches(String ie) {
                return ie != null && ie.contains("支出");
            }
        },
        NEUTRAL {
            @Override
            boolean matches(String ie) {
                return ie != null && ie.contains("中性");
            }
        },
        ALL {
            @Override
            boolean matches(String ie) {
                return true;
            }
        };

        abstract boolean matches(String ie);
    }
}
