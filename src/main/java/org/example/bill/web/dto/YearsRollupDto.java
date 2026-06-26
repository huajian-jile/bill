package org.example.bill.web.dto;

import java.util.List;

/**
 * 替代「按年 × 3 次 by-type」：单次响应含全量汇总 + 各自然年分桶，减轻客户端与连接数压力。
 */
public record YearsRollupDto(AllTimeTotalsDto allTime, List<YearBucketDto> byYear) {}
