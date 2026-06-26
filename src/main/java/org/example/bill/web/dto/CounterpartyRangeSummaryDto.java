package org.example.bill.web.dto;

import java.math.BigDecimal;

/** 全量时间范围内、所有交易对方汇总（用于侧栏，与分页表无关）。 */
public record CounterpartyRangeSummaryDto(
        long totalIncomeCount,
        long totalExpenseCount,
        long totalNeutralCount,
        BigDecimal totalIncomeAmount,
        BigDecimal totalExpenseAmount,
        BigDecimal totalNeutralAmount) {}
