package org.example.bill.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RealDataAnalyticsDto(
        LocalDate from,
        LocalDate to,
        BigDecimal realIncome,
        BigDecimal realExpense,
        BigDecimal realNeutral,
        long excludedPairTransactionCount,
        List<Long> excludedTransactionIds) {}
