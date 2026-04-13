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
            if ("PERM_USER_ADMIN".equals(ga.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据 channel 分发到微信或支付宝的 scope 解析。
     * - channel=wechat/merged：用 wechat_users.id 过滤
     * - channel=alipay：用 alipay_users.id 过滤
     */
    private List<Long> resolveScopeIds(Long wechatUserId, String wechatUserIdsCsv, String channel) {
        boolean admin = currentUserIsAdmin();
        if ("alipay".equalsIgnoreCase(channel)) {
            return analyticsScopeService.resolveAlipayUserIds(admin, wechatUserId, wechatUserIdsCsv);
        }
        // wechat / merged 都用微信用户维度
        return analyticsScopeService.resolveWechatUserIds(admin, wechatUserId, wechatUserIdsCsv);
    }

    /** 某日收支 */
    @GetMapping("/day")
    @PreAuthorize("isAuthenticated()")
    public DayAnalyticsDto day(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> ids = resolveScopeIds(wechatUserId, wechatUserIds, channel);
        return analyticsService.day(date, ids, AnalyticsChannel.fromParam(channel));
    }

    /** 某日收支明细 + 可选对比日（同一结构） */
    @GetMapping("/day-detail")
    @PreAuthorize("isAuthenticated()")
    public DayDetailDto dayDetail(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate compareDate,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> ids = resolveScopeIds(wechatUserId, wechatUserIds, channel);
        return analyticsService.dayDetail(date, compareDate, ids, AnalyticsChannel.fromParam(channel));
    }

    /** 近 30 天收入/支出明细（不含中性），用于中性区下方展示 */
    @GetMapping("/rolling-income-expense")
    @PreAuthorize("isAuthenticated()")
    public RollingIncomeExpenseDto rollingIncomeExpense(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> ids = resolveScopeIds(wechatUserId, wechatUserIds, channel);
        return analyticsService.rollingIncomeExpense(endDate, ids, AnalyticsChannel.fromParam(channel));
    }

    /** 某月每日收支 + 较前一日涨幅 */
    @GetMapping("/month")
    @PreAuthorize("isAuthenticated()")
    public List<MonthDailyRowDto> month(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> ids = resolveScopeIds(wechatUserId, wechatUserIds, channel);
        return analyticsService.month(year, month, ids, AnalyticsChannel.fromParam(channel));
    }

    /** 收入 / 支出 / 中性 汇总 */
    @GetMapping("/by-type")
    @PreAuthorize("isAuthenticated()")
    public TypeAnalyticsDto byType(
            @RequestParam String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> ids = resolveScopeIds(wechatUserId, wechatUserIds, channel);
        return analyticsService.byType(type, from, to, ids, AnalyticsChannel.fromParam(channel));
    }

    /** 真实收支：剔除同日同金额一收一支（转账退回） */
    @GetMapping("/real")
    @PreAuthorize("isAuthenticated()")
    public RealDataAnalyticsDto real(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) Long wechatUserId,
            @RequestParam(required = false) String wechatUserIds,
            @RequestParam(defaultValue = "wechat") String channel) {
        List<Long> ids = resolveScopeIds(wechatUserId, wechatUserIds, channel);
        return analyticsService.real(from, to, ids, AnalyticsChannel.fromParam(channel));
    }

    @GetMapping("/by-counterparty")
    @PreAuthorize("isAuthenticated()")
    public CounterpartyBoardDto byCounterparty(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel,
            @RequestParam(defaultValue = "false") boolean excludeRefundPairs) {
        List<Long> uids = resolveScopeIds(phoneId, phoneIds, channel);
        return analyticsService.counterpartyBoard(
                from, to, uids, AnalyticsChannel.fromParam(channel), excludeRefundPairs);
    }

    @GetMapping("/by-counterparty-detail")
    @PreAuthorize("isAuthenticated()")
    public List<TransactionBriefDto> byCounterpartyDetail(
            @RequestParam String counterparty,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long phoneId,
            @RequestParam(required = false) String phoneIds,
            @RequestParam(defaultValue = "wechat") String channel,
            @RequestParam(defaultValue = "false") boolean excludeRefundPairs) {
        List<Long> uids = resolveScopeIds(phoneId, phoneIds, channel);
        return analyticsService.counterpartyDetail(
                counterparty,
                from,
                to,
                uids,
                AnalyticsChannel.fromParam(channel),
                excludeRefundPairs);
    }
}
