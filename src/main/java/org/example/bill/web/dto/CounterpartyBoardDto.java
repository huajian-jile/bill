package org.example.bill.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CounterpartyBoardDto(
        List<CounterpartyGroupSummaryDto> groups,
        BigDecimal grandIncomeTotal,
        BigDecimal grandExpenseTotal,
        BigDecimal grandNeutralTotal) {}
