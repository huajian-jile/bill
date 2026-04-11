package org.example.bill.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DayAnalyticsDto(
        LocalDate date,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal neutralTotal,
        int incomeCount,
        int expenseCount,
        int neutralCount) {}
