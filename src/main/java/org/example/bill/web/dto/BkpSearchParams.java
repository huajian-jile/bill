package org.example.bill.web.dto;

import java.math.BigDecimal;

public record BkpSearchParams(
        String channel,
        Long billImportId,
        String tradeTimeFrom,
        String tradeTimeTo,
        String tradeType,
        String counterparty,
        String incomeExpense,
        BigDecimal amountMin,
        BigDecimal amountMax,
        int page,
        int size,
        String sort,
        String direction) {}
