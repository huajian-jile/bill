package org.example.bill.web.dto;

import java.math.BigDecimal;

public record TransactionBriefDto(
        Long id,
        String tradeTime,
        String tradeType,
        String counterparty,
        String product,
        String incomeExpense,
        BigDecimal amountYuan,
        String paymentMethod,
        String remark,
        /** wechat | alipay */
        String billChannel) {

    public TransactionBriefDto(
            Long id,
            String tradeTime,
            String tradeType,
            String counterparty,
            String product,
            String incomeExpense,
            BigDecimal amountYuan,
            String paymentMethod,
            String remark) {
        this(id, tradeTime, tradeType, counterparty, product, incomeExpense, amountYuan, paymentMethod, remark, "wechat");
    }
}
