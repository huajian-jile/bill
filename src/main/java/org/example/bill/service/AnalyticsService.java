package org.example.bill.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AlipayBillTransaction;
import org.example.bill.domain.WechatBillTransaction;
import org.example.bill.repo.AlipayBillTransactionRepository;
import org.example.bill.repo.WechatBillTransactionRepository;
import org.example.bill.util.TradeTimeUtil;
import org.example.bill.web.dto.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final WechatBillTransactionRepository txRepo;
    private final AlipayBillTransactionRepository alipayTxRepo;

    private enum IncomeExpenseKind {
        INCOME,
        EXPENSE,
        NEUTRAL,
        SKIP
    }

    private static IncomeExpenseKind classifyIncomeExpense(String ie) {
        if (ie == null) {
            return IncomeExpenseKind.SKIP;
        }
        if (ie.contains("收入")) {
            return IncomeExpenseKind.INCOME;
        }
        if (ie.contains("支出")) {
            return IncomeExpenseKind.EXPENSE;
        }
        if (ie.contains("中性")) {
            return IncomeExpenseKind.NEUTRAL;
        }
        return IncomeExpenseKind.SKIP;
    }

    /** 单参数 wechatUserId 或与逗号分隔 wechatUserIds 二选一；皆空表示不限制用户 */
    public static List<Long> resolveUserIds(Long wechatUserId, String wechatUserIdsCsv) {
        if (wechatUserIdsCsv != null && !wechatUserIdsCsv.isBlank()) {
            return Arrays.stream(wechatUserIdsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        }
        if (wechatUserId != null) {
            return List.of(wechatUserId);
        }
        return null;
    }

    public List<WechatBillTransaction> loadWechatTxs(List<Long> userIds) {
        List<WechatBillTransaction> raw;
        if (userIds == null) {
            raw = txRepo.findAll().stream().filter(t -> !t.isArchived()).toList();
        } else if (userIds.isEmpty()) {
            return List.of();
        } else if (userIds.size() == 1) {
            raw = txRepo.findActiveByWechatUserId(userIds.get(0));
        } else {
            raw = txRepo.findActiveByWechatUserIds(userIds);
        }
        return dedupeWechatByNaturalKey(raw);
    }

    /** 分析侧：同一手机号或 person + row_hash 只保留一条，修正历史重复导入。 */
    private static List<WechatBillTransaction> dedupeWechatByNaturalKey(
            List<WechatBillTransaction> txs) {
        Map<String, WechatBillTransaction> map = new LinkedHashMap<>();
        for (WechatBillTransaction t : txs) {
            String k = wechatNaturalKey(t);
            if (k == null) {
                map.putIfAbsent("id:" + t.getId(), t);
            } else {
                map.putIfAbsent(k, t);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static String wechatNaturalKey(WechatBillTransaction t) {
        if (t.getRowHash() == null || t.getRowHash().isBlank()) {
            return null;
        }
        String h = t.getRowHash().trim();
        if (t.getMobileCn() != null && !t.getMobileCn().isBlank()) {
            return "m:" + t.getMobileCn().trim() + ":" + h;
        }
        if (t.getPersonId() != null) {
            return "p:" + t.getPersonId() + ":" + h;
        }
        return null;
    }

    public List<AlipayBillTransaction> loadAlipayTxs(List<Long> userIds) {
        if (userIds == null) {
            return alipayTxRepo.findAll().stream().filter(t -> !t.isArchived()).toList();
        }
        if (userIds.isEmpty()) {
            return List.of();
        }
        return alipayTxRepo.findActiveByUserIds(userIds);
    }

    private static TransactionBriefDto toBriefWechat(WechatBillTransaction t) {
        return new TransactionBriefDto(
                t.getId(),
                t.getTradeTime(),
                t.getTradeType(),
                t.getCounterparty(),
                t.getProduct(),
                t.getIncomeExpense(),
                t.getAmountYuan(),
                t.getPaymentMethod(),
                t.getRemark(),
                "wechat");
    }

    private static TransactionBriefDto toBriefAlipay(AlipayBillTransaction t) {
        return new TransactionBriefDto(
                t.getId(),
                t.getTradeTime(),
                t.getTradeType(),
                t.getCounterparty(),
                t.getProduct(),
                t.getIncomeExpense(),
                t.getAmountYuan(),
                t.getPaymentMethod(),
                t.getRemark(),
                "alipay");
    }

    /** 某日汇总 + 按收/支/中性分组的明细 */
    public DaySliceDto buildDaySlice(LocalDate date, List<Long> userIds, AnalyticsChannel channel) {
        List<TransactionBriefDto> income = new ArrayList<>();
        List<TransactionBriefDto> expense = new ArrayList<>();
        List<TransactionBriefDto> neutral = new ArrayList<>();
        BigDecimal inc = BigDecimal.ZERO;
        BigDecimal exp = BigDecimal.ZERO;
        BigDecimal neu = BigDecimal.ZERO;
        if (channel == AnalyticsChannel.wechat || channel == AnalyticsChannel.merged) {
            for (WechatBillTransaction t : loadWechatTxs(userIds)) {
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty() || !od.get().equals(date) || t.getAmountYuan() == null) {
                    continue;
                }
                IncomeExpenseKind k = classifyIncomeExpense(t.getIncomeExpense());
                TransactionBriefDto brief = toBriefWechat(t);
                switch (k) {
                    case INCOME -> {
                        inc = inc.add(t.getAmountYuan());
                        income.add(brief);
                    }
                    case EXPENSE -> {
                        exp = exp.add(t.getAmountYuan());
                        expense.add(brief);
                    }
                    case NEUTRAL -> {
                        neu = neu.add(t.getAmountYuan());
                        neutral.add(brief);
                    }
                    default -> {
                    }
                }
            }
        }
        if (channel == AnalyticsChannel.alipay || channel == AnalyticsChannel.merged) {
            for (AlipayBillTransaction t : loadAlipayTxs(userIds)) {
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty() || !od.get().equals(date) || t.getAmountYuan() == null) {
                    continue;
                }
                IncomeExpenseKind k = classifyIncomeExpense(t.getIncomeExpense());
                TransactionBriefDto brief = toBriefAlipay(t);
                switch (k) {
                    case INCOME -> {
                        inc = inc.add(t.getAmountYuan());
                        income.add(brief);
                    }
                    case EXPENSE -> {
                        exp = exp.add(t.getAmountYuan());
                        expense.add(brief);
                    }
                    case NEUTRAL -> {
                        neu = neu.add(t.getAmountYuan());
                        neutral.add(brief);
                    }
                    default -> {
                    }
                }
            }
        }
        BigDecimal grand = inc.add(exp).add(neu);
        return new DaySliceDto(
                date,
                inc,
                exp,
                neu,
                grand,
                income.size(),
                expense.size(),
                neutral.size(),
                income,
                expense,
                neutral);
    }

    public DayDetailDto dayDetail(
            LocalDate date,
            LocalDate compareDate,
            Long wechatUserId,
            String wechatUserIds,
            String channelRaw) {
        List<Long> userIds = resolveUserIds(wechatUserId, wechatUserIds);
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        DaySliceDto day = buildDaySlice(date, userIds, channel);
        DaySliceDto compare =
                compareDate == null ? null : buildDaySlice(compareDate, userIds, channel);
        return new DayDetailDto(day, compare);
    }

    public RollingIncomeExpenseDto rollingIncomeExpense(
            LocalDate endDate,
            Long wechatUserId,
            String wechatUserIds,
            String channelRaw) {
        List<Long> userIds = resolveUserIds(wechatUserId, wechatUserIds);
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        LocalDate start = endDate.minusDays(29);
        List<TransactionBriefDto> income = new ArrayList<>();
        List<TransactionBriefDto> expense = new ArrayList<>();
        if (channel == AnalyticsChannel.wechat || channel == AnalyticsChannel.merged) {
            for (WechatBillTransaction t : loadWechatTxs(userIds)) {
                if (t.getAmountYuan() == null) {
                    continue;
                }
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty()) {
                    continue;
                }
                LocalDate d = od.get();
                if (d.isBefore(start) || d.isAfter(endDate)) {
                    continue;
                }
                IncomeExpenseKind k = classifyIncomeExpense(t.getIncomeExpense());
                if (k == IncomeExpenseKind.INCOME) {
                    income.add(toBriefWechat(t));
                } else if (k == IncomeExpenseKind.EXPENSE) {
                    expense.add(toBriefWechat(t));
                }
            }
        }
        if (channel == AnalyticsChannel.alipay || channel == AnalyticsChannel.merged) {
            for (AlipayBillTransaction t : loadAlipayTxs(userIds)) {
                if (t.getAmountYuan() == null) {
                    continue;
                }
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty()) {
                    continue;
                }
                LocalDate d = od.get();
                if (d.isBefore(start) || d.isAfter(endDate)) {
                    continue;
                }
                IncomeExpenseKind k = classifyIncomeExpense(t.getIncomeExpense());
                if (k == IncomeExpenseKind.INCOME) {
                    income.add(toBriefAlipay(t));
                } else if (k == IncomeExpenseKind.EXPENSE) {
                    expense.add(toBriefAlipay(t));
                }
            }
        }
        Comparator<String> tc = Comparator.nullsLast(String::compareTo);
        income.sort(Comparator.comparing(TransactionBriefDto::tradeTime, tc));
        expense.sort(Comparator.comparing(TransactionBriefDto::tradeTime, tc));
        return new RollingIncomeExpenseDto(start, endDate, income, expense);
    }

    public DayAnalyticsDto day(
            LocalDate date,
            Long wechatUserId,
            String wechatUserIds,
            String channelRaw) {
        List<Long> userIds = resolveUserIds(wechatUserId, wechatUserIds);
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        DaySliceDto s = buildDaySlice(date, userIds, channel);
        return new DayAnalyticsDto(
                s.date(),
                s.incomeTotal(),
                s.expenseTotal(),
                s.neutralTotal(),
                s.incomeCount(),
                s.expenseCount(),
                s.neutralCount());
    }

    public List<MonthDailyRowDto> month(
            int year, int month, Long wechatUserId, String wechatUserIds, String channelRaw) {
        List<Long> userIds = resolveUserIds(wechatUserId, wechatUserIds);
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Map<LocalDate, BigDecimal[]> sums = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            sums.put(d, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
        }
        if (channel == AnalyticsChannel.wechat || channel == AnalyticsChannel.merged) {
            for (WechatBillTransaction t : loadWechatTxs(userIds)) {
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty()) {
                    continue;
                }
                LocalDate d = od.get();
                if (d.isBefore(start) || d.isAfter(end)) {
                    continue;
                }
                if (t.getAmountYuan() == null) {
                    continue;
                }
                BigDecimal[] arr = sums.get(d);
                if (arr == null) {
                    continue;
                }
                String ie = t.getIncomeExpense();
                if (ie != null && ie.contains("收入")) {
                    arr[0] = arr[0].add(t.getAmountYuan());
                } else if (ie != null && ie.contains("支出")) {
                    arr[1] = arr[1].add(t.getAmountYuan());
                } else if (ie != null && ie.contains("中性")) {
                    arr[2] = arr[2].add(t.getAmountYuan());
                }
            }
        }
        if (channel == AnalyticsChannel.alipay || channel == AnalyticsChannel.merged) {
            for (AlipayBillTransaction t : loadAlipayTxs(userIds)) {
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty()) {
                    continue;
                }
                LocalDate d = od.get();
                if (d.isBefore(start) || d.isAfter(end)) {
                    continue;
                }
                if (t.getAmountYuan() == null) {
                    continue;
                }
                BigDecimal[] arr = sums.get(d);
                if (arr == null) {
                    continue;
                }
                String ie = t.getIncomeExpense();
                if (ie != null && ie.contains("收入")) {
                    arr[0] = arr[0].add(t.getAmountYuan());
                } else if (ie != null && ie.contains("支出")) {
                    arr[1] = arr[1].add(t.getAmountYuan());
                } else if (ie != null && ie.contains("中性")) {
                    arr[2] = arr[2].add(t.getAmountYuan());
                }
            }
        }
        List<MonthDailyRowDto> out = new ArrayList<>();
        BigDecimal prevInc = null;
        BigDecimal prevExp = null;
        for (Map.Entry<LocalDate, BigDecimal[]> e : sums.entrySet()) {
            BigDecimal[] v = e.getValue();
            BigDecimal gInc = growthPct(prevInc, v[0]);
            BigDecimal gExp = growthPct(prevExp, v[1]);
            out.add(
                    new MonthDailyRowDto(
                            e.getKey(), v[0], v[1], v[2], gInc, gExp));
            prevInc = v[0];
            prevExp = v[1];
        }
        return out;
    }

    private static BigDecimal growthPct(BigDecimal previous, BigDecimal current) {
        if (previous == null) {
            return null;
        }
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    public TypeAnalyticsDto byType(
            String type,
            LocalDate from,
            LocalDate to,
            Long wechatUserId,
            String wechatUserIds,
            String channelRaw) {
        RefundPairFinder.IncomeExpenseFilter f =
                switch (type.toLowerCase()) {
                    case "income" -> RefundPairFinder.IncomeExpenseFilter.INCOME;
                    case "expense" -> RefundPairFinder.IncomeExpenseFilter.EXPENSE;
                    case "neutral" -> RefundPairFinder.IncomeExpenseFilter.NEUTRAL;
                    default -> throw new IllegalArgumentException("type 应为 income|expense|neutral");
                };
        List<Long> userIds = resolveUserIds(wechatUserId, wechatUserIds);
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        BigDecimal total = BigDecimal.ZERO;
        long cnt = 0;
        if (channel == AnalyticsChannel.wechat || channel == AnalyticsChannel.merged) {
            for (WechatBillTransaction t : loadWechatTxs(userIds)) {
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty()) {
                    continue;
                }
                LocalDate d = od.get();
                if (from != null && d.isBefore(from)) {
                    continue;
                }
                if (to != null && d.isAfter(to)) {
                    continue;
                }
                if (t.getAmountYuan() == null) {
                    continue;
                }
                if (!f.matches(t.getIncomeExpense())) {
                    continue;
                }
                total = total.add(t.getAmountYuan());
                cnt++;
            }
        }
        if (channel == AnalyticsChannel.alipay || channel == AnalyticsChannel.merged) {
            for (AlipayBillTransaction t : loadAlipayTxs(userIds)) {
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty()) {
                    continue;
                }
                LocalDate d = od.get();
                if (from != null && d.isBefore(from)) {
                    continue;
                }
                if (to != null && d.isAfter(to)) {
                    continue;
                }
                if (t.getAmountYuan() == null) {
                    continue;
                }
                if (!f.matches(t.getIncomeExpense())) {
                    continue;
                }
                total = total.add(t.getAmountYuan());
                cnt++;
            }
        }
        return new TypeAnalyticsDto(type, from, to, total, cnt);
    }

    public RealDataAnalyticsDto real(
            LocalDate from, LocalDate to, Long wechatUserId, String wechatUserIds, String channelRaw) {
        List<Long> userIds = resolveUserIds(wechatUserId, wechatUserIds);
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        BigDecimal inc = BigDecimal.ZERO;
        BigDecimal exp = BigDecimal.ZERO;
        BigDecimal neu = BigDecimal.ZERO;
        int excludedCount = 0;
        List<Long> excludedIds = new ArrayList<>();
        if (channel == AnalyticsChannel.wechat || channel == AnalyticsChannel.merged) {
            List<WechatBillTransaction> inRange = filterWechatInRange(loadWechatTxs(userIds), from, to);
            Set<Long> excluded = RefundPairFinder.findPairedTransactionIds(inRange);
            excludedCount += excluded.size();
            excludedIds.addAll(excluded.stream().sorted().toList());
            inc =
                    inc.add(
                            RefundPairFinder.sumAmount(
                                    inRange, excluded, RefundPairFinder.IncomeExpenseFilter.INCOME));
            exp =
                    exp.add(
                            RefundPairFinder.sumAmount(
                                    inRange, excluded, RefundPairFinder.IncomeExpenseFilter.EXPENSE));
            neu =
                    neu.add(
                            RefundPairFinder.sumAmount(
                                    inRange, excluded, RefundPairFinder.IncomeExpenseFilter.NEUTRAL));
        }
        if (channel == AnalyticsChannel.alipay || channel == AnalyticsChannel.merged) {
            List<AlipayBillTransaction> inRangeA = filterAlipayInRange(loadAlipayTxs(userIds), from, to);
            Set<Long> excludedA = RefundPairFinder.findPairedAlipayTransactionIds(inRangeA);
            excludedCount += excludedA.size();
            excludedIds.addAll(excludedA.stream().sorted().toList());
            inc =
                    inc.add(
                            RefundPairFinder.sumAlipayAmount(
                                    inRangeA, excludedA, RefundPairFinder.IncomeExpenseFilter.INCOME));
            exp =
                    exp.add(
                            RefundPairFinder.sumAlipayAmount(
                                    inRangeA, excludedA, RefundPairFinder.IncomeExpenseFilter.EXPENSE));
            neu =
                    neu.add(
                            RefundPairFinder.sumAlipayAmount(
                                    inRangeA, excludedA, RefundPairFinder.IncomeExpenseFilter.NEUTRAL));
        }
        Collections.sort(excludedIds);
        return new RealDataAnalyticsDto(
                from, to, inc, exp, neu, excludedCount, excludedIds);
    }

    private static List<WechatBillTransaction> filterWechatInRange(
            List<WechatBillTransaction> txs, LocalDate from, LocalDate to) {
        List<WechatBillTransaction> inRange = new ArrayList<>();
        for (WechatBillTransaction t : txs) {
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) {
                continue;
            }
            LocalDate d = od.get();
            if (from != null && d.isBefore(from)) {
                continue;
            }
            if (to != null && d.isAfter(to)) {
                continue;
            }
            inRange.add(t);
        }
        return inRange;
    }

    private static List<AlipayBillTransaction> filterAlipayInRange(
            List<AlipayBillTransaction> txs, LocalDate from, LocalDate to) {
        List<AlipayBillTransaction> inRange = new ArrayList<>();
        for (AlipayBillTransaction t : txs) {
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) {
                continue;
            }
            LocalDate d = od.get();
            if (from != null && d.isBefore(from)) {
                continue;
            }
            if (to != null && d.isAfter(to)) {
                continue;
            }
            inRange.add(t);
        }
        return inRange;
    }
}
