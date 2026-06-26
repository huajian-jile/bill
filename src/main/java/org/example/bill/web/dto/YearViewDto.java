package org.example.bill.web.dto;

import java.util.List;

/**
 * 「某年」视图：该自然年汇总 + 12 个月每日行（月份下标 0=1 月 … 11=12 月），一次 HTTP 替代 3×by-type + 12×month。
 */
public record YearViewDto(int year, YearBucketDto totals, List<List<MonthDailyRowDto>> months) {}
