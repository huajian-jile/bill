package org.example.bill.web.dto;

import java.math.BigDecimal;

/** 某一自然年内的收/支/中性汇总。 */
public record YearBucketDto(
        int year,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal neutralTotal,
        long incomeCount,
        long expenseCount,
        long neutralCount) {}
