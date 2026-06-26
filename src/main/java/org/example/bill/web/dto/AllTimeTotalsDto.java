package org.example.bill.web.dto;

import java.math.BigDecimal;

/**
 * 全时段收入/支出/中性 金额与笔数（与三次 {@code by-type} 无日期 等价）。
 */
public record AllTimeTotalsDto(
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal neutralTotal,
        long incomeCount,
        long expenseCount,
        long neutralCount) {}
