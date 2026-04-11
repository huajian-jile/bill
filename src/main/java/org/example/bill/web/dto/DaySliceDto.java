package org.example.bill.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DaySliceDto(
        LocalDate date,
        BigDecimal incomeTotal,
        BigDecimal expenseTotal,
        BigDecimal neutralTotal,
        /** 收入 + 支出 + 中性（当日流水合计） */
        BigDecimal grandTotal,
        int incomeCount,
        int expenseCount,
        int neutralCount,
        List<TransactionBriefDto> incomeTransactions,
        List<TransactionBriefDto> expenseTransactions,
        List<TransactionBriefDto> neutralTransactions) {}
