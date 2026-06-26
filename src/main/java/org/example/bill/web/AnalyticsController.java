package org.example.bill.web;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.AnalyticsChannel;
import org.example.bill.service.AnalyticsScopeService;
import org.example.bill.service.AnalyticsService;
import org.example.bill.web.dto.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsScopeService analyticsScopeService;

    private static boolean currentUserIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        for (GrantedAuthority ga : auth.getAuthorities()) {
            if ("PERM_VIEW_ALL_BILLS".equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** 某日收支 */
    @GetMapping("/day")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public DayAnalyticsDto day(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.day(date, uids, channel);
    }

    /** 某日收支明细 + 可选对比日（同一结构） */
    @GetMapping("/day-detail")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public DayDetailDto dayDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate compareDate,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.dayDetail(date, compareDate, uids, channel);
    }

    /** 近 30 天收入/支出明细（不含中性），用于中性区下方展示 */
    @GetMapping("/rolling-income-expense")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public RollingIncomeExpenseDto rollingIncomeExpense(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.rollingIncomeExpense(endDate, uids, channel);
    }

    /** 某月每日收支 + 较前一日涨幅 */
    @GetMapping("/month")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public List<MonthDailyRowDto> month(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.month(year, month, uids, channel);
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
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.byType(type, from, to, uids, channel);
    }

    /**
     * 全量汇总 + 按自然年分桶（各年收/支/中性金额与笔数）。供「全部」时间范围一请求替代 年数×3 次 by-type。
     */
    @GetMapping("/years-rollup")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public YearsRollupDto yearsRollup(
            @RequestParam(defaultValue = "2000") int fromYear,
            @RequestParam(required = false) Integer toYear,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        int to = toYear == null ? LocalDate.now().getYear() : toYear;
        return analyticsService.yearsRollup(fromYear, to, uids, channel);
    }

    /**
     * 「某年」：年度汇总 + 12 个月每日汇总，一次响应（替代 3 次 by-type + 12 次 month）。
     */
    @GetMapping("/year-view")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public YearViewDto yearView(
            @RequestParam int year,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.yearView(year, uids, channel);
    }

    /** 真实收支：剔除同日同金额一收一支（转账退回） */
    @GetMapping("/real")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public RealDataAnalyticsDto real(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> uids = analyticsScopeService.resolveWechatUserIds(currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.real(from, to, uids, channel);
    }

    @GetMapping("/by-counterparty")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public CounterpartyBoardDto byCounterparty(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel,
            @RequestParam(defaultValue = "false") boolean excludeRefundPairs,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<Long> uids =
                analyticsScopeService.resolveWechatUserIds(
                        currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.counterpartyBoard(
                from, to, uids, AnalyticsChannel.fromParam(channel), excludeRefundPairs, page, size);
    }

    @GetMapping("/by-counterparty-detail")
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public List<TransactionBriefDto> byCounterpartyDetail(
            @RequestParam String counterparty,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel,
            @RequestParam(defaultValue = "false") boolean excludeRefundPairs) {
        List<Long> uids =
                analyticsScopeService.resolveWechatUserIds(
                        currentUserIsAdmin(), phoneId, phoneIds);
        return analyticsService.counterpartyDetail(
                counterparty,
                from,
                to,
                uids,
                AnalyticsChannel.fromParam(channel),
                excludeRefundPairs);
    }
}
