package org.example.bill.web.dto;

import java.time.LocalDate;
import java.util.List;

/** 截止 endDate 的近 30 天（含当日）收入/支出明细（不含中性） */
public record RollingIncomeExpenseDto(
        LocalDate rangeStart,
        LocalDate rangeEnd,
        List<TransactionBriefDto> incomeTransactions,
        List<TransactionBriefDto> expenseTransactions) {}
