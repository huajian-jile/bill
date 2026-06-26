package org.example.bill.service.bkp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.example.bill.domain.BkpWechatBillTransaction;
import org.example.bill.web.dto.BkpSearchParams;

public final class BkpQuery {

    private BkpQuery() {}

    public static LambdaQueryWrapper<BkpWechatBillTransaction> wrap(BkpSearchParams p) {
        LambdaQueryWrapper<BkpWechatBillTransaction> w = Wrappers.lambdaQuery();
        if (p.channel() != null
                && !p.channel().isBlank()
                && !"ALL".equalsIgnoreCase(p.channel())) {
            w.eq(BkpWechatBillTransaction::getBillChannel, p.channel().trim().toUpperCase());
        }
        if (p.billImportId() != null) {
            w.eq(BkpWechatBillTransaction::getBillImportId, p.billImportId());
        }
        String tf = normalizeStart(p.tradeTimeFrom());
        String tt = normalizeEnd(p.tradeTimeTo());
        if (tf != null) {
            w.ge(BkpWechatBillTransaction::getTradeTime, tf);
        }
        if (tt != null) {
            w.le(BkpWechatBillTransaction::getTradeTime, tt);
        }
        if (p.tradeType() != null && !p.tradeType().isBlank()) {
            w.apply(
                    "LOWER(trade_type) LIKE {0}",
                    "%" + p.tradeType().trim().toLowerCase().replace("%", "\\%") + "%");
        }
        if (p.counterparty() != null && !p.counterparty().isBlank()) {
            w.apply(
                    "LOWER(counterparty) LIKE {0}",
                    "%" + p.counterparty().trim().toLowerCase().replace("%", "\\%") + "%");
        }
        if (p.incomeExpense() != null && !p.incomeExpense().isBlank()) {
            w.apply(
                    "LOWER(income_expense) LIKE {0}",
                    "%" + p.incomeExpense().trim().toLowerCase().replace("%", "\\%") + "%");
        }
        if (p.amountMin() != null) {
            w.ge(BkpWechatBillTransaction::getAmountYuan, p.amountMin());
        }
        if (p.amountMax() != null) {
            w.le(BkpWechatBillTransaction::getAmountYuan, p.amountMax());
        }
        boolean asc = "asc".equalsIgnoreCase(p.direction());
        String sf = p.sort() == null ? "tradeTime" : p.sort().trim();
        switch (sf) {
            case "tradeType" -> w.orderBy(true, asc, BkpWechatBillTransaction::getTradeType);
            case "counterparty" -> w.orderBy(true, asc, BkpWechatBillTransaction::getCounterparty);
            case "incomeExpense" -> w.orderBy(true, asc, BkpWechatBillTransaction::getIncomeExpense);
            case "amountYuan" -> w.orderBy(true, asc, BkpWechatBillTransaction::getAmountYuan);
            case "tradeTime" -> w.orderBy(true, asc, BkpWechatBillTransaction::getTradeTime);
            default -> w.orderBy(true, asc, BkpWechatBillTransaction::getTradeTime);
        }
        return w;
    }

    private static String normalizeStart(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        s = s.trim();
        if (s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            return s + " 00:00:00";
        }
        return s;
    }

    private static String normalizeEnd(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        s = s.trim();
        if (s.length() == 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
            return s + " 23:59:59";
        }
        return s;
    }
}
