package org.example.bill.web.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 交易对方看板。{@code groups} 为当前页；{@code chartTop} 为全量中用于柱状的 TopN；{@code
 * summary} 为全量笔数/金额侧栏用。
 */
public record CounterpartyBoardDto(
        List<CounterpartyGroupSummaryDto> groups,
        BigDecimal grandIncomeTotal,
        BigDecimal grandExpenseTotal,
        BigDecimal grandNeutralTotal,
        long totalGroupCount,
        int page,
        int pageSize,
        List<CounterpartyGroupSummaryDto> chartTop,
        CounterpartyRangeSummaryDto summary) {}
