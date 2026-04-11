package org.example.bill.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TypeAnalyticsDto(
        String type,
        LocalDate from,
        LocalDate to,
        BigDecimal totalAmount,
        long transactionCount) {}
