package org.example.bill.web;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.AnalyticsService;
import org.example.bill.web.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** 某日收支 */
    @GetMapping("/day")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public DayAnalyticsDto day(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        return analyticsService.day(date, wechatUserId, wechatUserIds, channel);
    }

    /** 某日收支明细 + 可选对比日（同一结构） */
    @GetMapping("/day-detail")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public DayDetailDto dayDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate compareDate,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        return analyticsService.dayDetail(date, compareDate, wechatUserId, wechatUserIds, channel);
    }

    /** 近 30 天收入/支出明细（不含中性），用于中性区下方展示 */
    @GetMapping("/rolling-income-expense")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public RollingIncomeExpenseDto rollingIncomeExpense(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        return analyticsService.rollingIncomeExpense(endDate, wechatUserId, wechatUserIds, channel);
    }

    /** 某月每日收支 + 较前一日涨幅 */
    @GetMapping("/month")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public List<MonthDailyRowDto> month(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        return analyticsService.month(year, month, wechatUserId, wechatUserIds, channel);
    }

    /** 收入 / 支出 / 中性 汇总 */
    @GetMapping("/by-type")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public TypeAnalyticsDto byType(
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        return analyticsService.byType(type, from, to, wechatUserId, wechatUserIds, channel);
    }

    /** 真实收支：剔除同日同金额一收一支（转账退回） */
    @GetMapping("/real")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public RealDataAnalyticsDto real(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        return analyticsService.real(from, to, wechatUserId, wechatUserIds, channel);
    }
}
