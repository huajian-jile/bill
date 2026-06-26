package org.example.bill.web.dto;

import java.math.BigDecimal;

/**
 * 按「交易对方」聚合一行：统计该对方在筛选条件下的收入/支出/中性及最近一笔时间。
 */
public record CounterpartyGroupSummaryDto(
        String counterparty,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal neutralTotal,
        int incomeCount,
        int expenseCount,
        int neutralCount,
        /** 该组内最近一笔交易时间（原始字符串，可为 null） */
        String lastTradeTime) {}
