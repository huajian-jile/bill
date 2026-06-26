package org.example.bill.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MonthDailyRowDto(
        LocalDate date,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal neutralTotal,
        BigDecimal incomeGrowthPercent,
        BigDecimal expenseGrowthPercent,
        int incomeCount,
        int expenseCount,
        int neutralCount) {}
