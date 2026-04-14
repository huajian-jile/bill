package org.example.bill.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.WechatBillTransaction;
import org.example.bill.repo.WechatBillTransactionRepository;
import org.example.bill.util.TradeTimeUtil;
import org.example.bill.web.dto.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final WechatBillTransactionRepository txRepo;

    private enum IncomeExpenseKind {
        INCOME, EXPENSE, NEUTRAL, SKIP
    }

    private static IncomeExpenseKind classifyIncomeExpense(String ie) {
        if (ie == null) return IncomeExpenseKind.SKIP;
        if (ie.contains("收入")) return IncomeExpenseKind.INCOME;
        if (ie.contains("支出")) return IncomeExpenseKind.EXPENSE;
        if (ie.contains("中性")) return IncomeExpenseKind.NEUTRAL;
        return IncomeExpenseKind.SKIP;
    }

    public static List<Long> resolveUserIds(Long wechatUserId, String wechatUserIdsCsv) {
        if (wechatUserIdsCsv != null && !wechatUserIdsCsv.isBlank()) {
            return Arrays.stream(wechatUserIdsCsv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .toList();
        }
        if (wechatUserId != null) return List.of(wechatUserId);
        return null;
    }

    /**
     * 按 channel 加载交易，channel 为 null 时加载所有。
     * 分析侧：同一手机号或 person + row_hash 只保留一条，修正历史重复导入。
     */
    public List<WechatBillTransaction> loadTxsByChannel(List<Long> userIds, String channel) {
        List<WechatBillTransaction> raw;
        if (userIds == null) {
            raw = txRepo.findAll().stream().filter(t -> !Boolean.TRUE.equals(t.getArchived())).toList();
        } else if (userIds.isEmpty()) {
            return List.of();
        } else {
            raw = txRepo.findActiveByWechatUserIds(userIds);
        }
        List<WechatBillTransaction> deduped = dedupeByNaturalKey(raw);
        if (channel == null) return deduped;
        return deduped.stream().filter(t -> channel.equals(t.getChannel())).toList();
    }

    private static List<WechatBillTransaction> dedupeByNaturalKey(List<WechatBillTransaction> txs) {
        Map<String, WechatBillTransaction> map = new LinkedHashMap<>();
        for (WechatBillTransaction t : txs) {
            String k = naturalKey(t);
            if (k == null) {
                map.putIfAbsent("id:" + t.getId(), t);
            } else {
                map.putIfAbsent(k, t);
            }
        }
        return new ArrayList<>(map.values());
    }

    private static String naturalKey(WechatBillTransaction t) {
        if (t.getRowHash() == null || t.getRowHash().isBlank()) return null;
        String h = t.getRowHash().trim();
        if (t.getMobileCn() != null && !t.getMobileCn().isBlank()) {
            return "m:" + t.getMobileCn().trim() + ":" + h;
        }
        if (t.getPersonId() != null) return "p:" + t.getPersonId() + ":" + h;
        return null;
    }

    private static TransactionBriefDto toBrief(WechatBillTransaction t) {
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
                t.getChannel() != null && t.getChannel().equalsIgnoreCase("ALIPAY") ? "alipay" : "wechat");
    }

    /** 某日汇总 + 按收/支/中性分组的明细 */
    public DaySliceDto buildDaySlice(LocalDate date, List<Long> userIds, AnalyticsChannel channel) {
        List<TransactionBriefDto> income = new ArrayList<>();
        List<TransactionBriefDto> expense = new ArrayList<>();
        List<TransactionBriefDto> neutral = new ArrayList<>();
        BigDecimal inc = BigDecimal.ZERO;
        BigDecimal exp = BigDecimal.ZERO;
        BigDecimal neu = BigDecimal.ZERO;

        String ch = channel == AnalyticsChannel.wechat ? "WECHAT"
                  : channel == AnalyticsChannel.alipay ? "ALIPAY"
                  : null;

        for (WechatBillTransaction t : loadTxsByChannel(userIds, ch)) {
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty() || !od.get().equals(date) || t.getAmountYuan() == null) continue;
            IncomeExpenseKind k = classifyIncomeExpense(t.getIncomeExpense());
            TransactionBriefDto brief = toBrief(t);
            switch (k) {
                case INCOME -> { inc = inc.add(t.getAmountYuan()); income.add(brief); }
                case EXPENSE -> { exp = exp.add(t.getAmountYuan()); expense.add(brief); }
                case NEUTRAL -> { neu = neu.add(t.getAmountYuan()); neutral.add(brief); }
                default -> {}
            }
        }

        BigDecimal grand = inc.add(exp).add(neu);
        return new DaySliceDto(date, inc, exp, neu, grand,
                income.size(), expense.size(), neutral.size(),
                income, expense, neutral);
    }

    public DayDetailDto dayDetail(
            LocalDate date, LocalDate compareDate, List<Long> userIds, String channelRaw) {
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        DaySliceDto day = buildDaySlice(date, userIds, channel);
        DaySliceDto compare = compareDate == null ? null : buildDaySlice(compareDate, userIds, channel);
        return new DayDetailDto(day, compare);
    }

    public RollingIncomeExpenseDto rollingIncomeExpense(
            LocalDate endDate, List<Long> userIds, String channelRaw) {
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        LocalDate start = endDate.minusDays(29);
        List<TransactionBriefDto> income = new ArrayList<>();
        List<TransactionBriefDto> expense = new ArrayList<>();

        String ch = channel == AnalyticsChannel.wechat ? "WECHAT"
                  : channel == AnalyticsChannel.alipay ? "ALIPAY"
                  : null;

        for (WechatBillTransaction t : loadTxsByChannel(userIds, ch)) {
            if (t.getAmountYuan() == null) continue;
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) continue;
            LocalDate d = od.get();
            if (d.isBefore(start) || d.isAfter(endDate)) continue;
            IncomeExpenseKind k = classifyIncomeExpense(t.getIncomeExpense());
            if (k == IncomeExpenseKind.INCOME) income.add(toBrief(t));
            else if (k == IncomeExpenseKind.EXPENSE) expense.add(toBrief(t));
        }
        Comparator<String> tc = Comparator.nullsLast(String::compareTo);
        income.sort(Comparator.comparing(TransactionBriefDto::tradeTime, tc));
        expense.sort(Comparator.comparing(TransactionBriefDto::tradeTime, tc));
        return new RollingIncomeExpenseDto(start, endDate, income, expense);
    }

    public DayAnalyticsDto day(LocalDate date, List<Long> userIds, String channelRaw) {
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        DaySliceDto s = buildDaySlice(date, userIds, channel);
        return new DayAnalyticsDto(
                s.date(), s.incomeTotal(), s.expenseTotal(), s.neutralTotal(),
                s.incomeCount(), s.expenseCount(), s.neutralCount());
    }

    public List<MonthDailyRowDto> month(
            int year, int month, List<Long> userIds, String channelRaw) {
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Map<LocalDate, BigDecimal[]> sums = new TreeMap<>();
        Map<LocalDate, int[]> txnCounts = new TreeMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            sums.put(d, new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            txnCounts.put(d, new int[3]);
        }

        String ch = channel == AnalyticsChannel.wechat ? "WECHAT"
                  : channel == AnalyticsChannel.alipay ? "ALIPAY"
                  : null;

        for (WechatBillTransaction t : loadTxsByChannel(userIds, ch)) {
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) continue;
            LocalDate d = od.get();
            if (d.isBefore(start) || d.isAfter(end)) continue;
            if (t.getAmountYuan() == null) continue;
            BigDecimal[] arr = sums.get(d);
            int[] cnt = txnCounts.get(d);
            if (arr == null || cnt == null) continue;
            String ie = t.getIncomeExpense();
            if (ie != null && ie.contains("收入")) { arr[0] = arr[0].add(t.getAmountYuan()); cnt[0]++; }
            else if (ie != null && ie.contains("支出")) { arr[1] = arr[1].add(t.getAmountYuan()); cnt[1]++; }
            else if (ie != null && ie.contains("中性")) { arr[2] = arr[2].add(t.getAmountYuan()); cnt[2]++; }
        }

        List<MonthDailyRowDto> out = new ArrayList<>();
        BigDecimal prevInc = null;
        BigDecimal prevExp = null;
        for (Map.Entry<LocalDate, BigDecimal[]> e : sums.entrySet()) {
            BigDecimal[] v = e.getValue();
            int[] c = txnCounts.getOrDefault(e.getKey(), new int[3]);
            out.add(new MonthDailyRowDto(e.getKey(), v[0], v[1], v[2],
                    growthPct(prevInc, v[0]), growthPct(prevExp, v[1]), c[0], c[1], c[2]));
            prevInc = v[0];
            prevExp = v[1];
        }
        return out;
    }

    private static BigDecimal growthPct(BigDecimal previous, BigDecimal current) {
        if (previous == null) return null;
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO : null;
        }
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    public TypeAnalyticsDto byType(
            String type, LocalDate from, LocalDate to,
            List<Long> userIds, String channelRaw) {
        RefundPairFinder.IncomeExpenseFilter f = switch (type.toLowerCase()) {
            case "income" -> RefundPairFinder.IncomeExpenseFilter.INCOME;
            case "expense" -> RefundPairFinder.IncomeExpenseFilter.EXPENSE;
            case "neutral" -> RefundPairFinder.IncomeExpenseFilter.NEUTRAL;
            default -> throw new IllegalArgumentException("type 应为 income|expense|neutral");
        };
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        BigDecimal total = BigDecimal.ZERO;
        long cnt = 0;

        String ch = channel == AnalyticsChannel.wechat ? "WECHAT"
                  : channel == AnalyticsChannel.alipay ? "ALIPAY"
                  : null;

        for (WechatBillTransaction t : loadTxsByChannel(userIds, ch)) {
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) continue;
            LocalDate d = od.get();
            if (from != null && d.isBefore(from)) continue;
            if (to != null && d.isAfter(to)) continue;
            if (t.getAmountYuan() == null) continue;
            if (!f.matches(t.getIncomeExpense())) continue;
            total = total.add(t.getAmountYuan());
            cnt++;
        }
        return new TypeAnalyticsDto(type, from, to, total, cnt);
    }

    public RealDataAnalyticsDto real(
            LocalDate from, LocalDate to,
            List<Long> userIds, String channelRaw) {
        AnalyticsChannel channel = AnalyticsChannel.fromParam(channelRaw);
        BigDecimal inc = BigDecimal.ZERO;
        BigDecimal exp = BigDecimal.ZERO;
        BigDecimal neu = BigDecimal.ZERO;
        int excludedCount = 0;
        List<Long> excludedIds = new ArrayList<>();

        String ch = channel == AnalyticsChannel.wechat ? "WECHAT"
                  : channel == AnalyticsChannel.alipay ? "ALIPAY"
                  : null;

        List<WechatBillTransaction> inRange = filterInRange(loadTxsByChannel(userIds, ch), from, to);
        Set<Long> excluded = RefundPairFinder.findPairedTransactionIds(inRange);
        excludedCount += excluded.size();
        excludedIds.addAll(excluded.stream().sorted().toList());
        inc = inc.add(RefundPairFinder.sumAmount(inRange, excluded, RefundPairFinder.IncomeExpenseFilter.INCOME));
        exp = exp.add(RefundPairFinder.sumAmount(inRange, excluded, RefundPairFinder.IncomeExpenseFilter.EXPENSE));
        neu = neu.add(RefundPairFinder.sumAmount(inRange, excluded, RefundPairFinder.IncomeExpenseFilter.NEUTRAL));

        Collections.sort(excludedIds);
        return new RealDataAnalyticsDto(from, to, inc, exp, neu, excludedCount, excludedIds);
    }

    /** 按交易对方聚合 */
    public CounterpartyBoardDto counterpartyBoard(
            LocalDate from, LocalDate to, List<Long> wechatUserIds,
            AnalyticsChannel channel, boolean excludeRefundPairs) {
        Map<String, CounterpartyAgg> map = new HashMap<>();

        String ch = channel == AnalyticsChannel.wechat ? "WECHAT"
                  : channel == AnalyticsChannel.alipay ? "ALIPAY"
                  : null;

        List<WechatBillTransaction> inRange = filterInRange(loadTxsByChannel(wechatUserIds, ch), from, to);
        Set<Long> excluded = excludeRefundPairs
                ? RefundPairFinder.findPairedTransactionIds(inRange)
                : Set.of();
        for (WechatBillTransaction t : inRange) {
            if (excludeRefundPairs && excluded.contains(t.getId())) continue;
            if (t.getAmountYuan() == null) continue;
            IncomeExpenseKind k = classifyIncomeExpense(t.getIncomeExpense());
            if (k == IncomeExpenseKind.SKIP) continue;
            String ck = counterpartyKey(t.getCounterparty());
            map.computeIfAbsent(ck, x -> new CounterpartyAgg()).add(t, k);
        }

        BigDecimal grandI = BigDecimal.ZERO, grandE = BigDecimal.ZERO, grandN = BigDecimal.ZERO;
        List<CounterpartyGroupSummaryDto> groups = new ArrayList<>();
        for (Map.Entry<String, CounterpartyAgg> e : map.entrySet()) {
            CounterpartyAgg a = e.getValue();
            String label = e.getKey().isEmpty() ? "（未填写）" : e.getKey();
            groups.add(new CounterpartyGroupSummaryDto(
                    label, a.incomeTotal, a.expenseTotal, a.neutralTotal,
                    a.incomeCount, a.expenseCount, a.neutralCount, a.lastTradeTime));
            grandI = grandI.add(a.incomeTotal);
            grandE = grandE.add(a.expenseTotal);
            grandN = grandN.add(a.neutralTotal);
        }
        groups.sort(Comparator.comparing(CounterpartyGroupSummaryDto::expenseTotal)
                .reversed()
                .thenComparing(CounterpartyGroupSummaryDto::counterparty));
        return new CounterpartyBoardDto(groups, grandI, grandE, grandN);
    }

    public List<TransactionBriefDto> counterpartyDetail(
            String counterpartyQuery, LocalDate from, LocalDate to,
            List<Long> wechatUserIds, AnalyticsChannel channel, boolean excludeRefundPairs) {
        List<TransactionBriefDto> out = new ArrayList<>();

        String ch = channel == AnalyticsChannel.wechat ? "WECHAT"
                  : channel == AnalyticsChannel.alipay ? "ALIPAY"
                  : null;

        List<WechatBillTransaction> inRange = filterInRange(loadTxsByChannel(wechatUserIds, ch), from, to);
        Set<Long> excluded = excludeRefundPairs
                ? RefundPairFinder.findPairedTransactionIds(inRange)
                : Set.of();
        for (WechatBillTransaction t : inRange) {
            if (excludeRefundPairs && excluded.contains(t.getId())) continue;
            if (!counterpartyMatches(t.getCounterparty(), counterpartyQuery)) continue;
            out.add(toBrief(t));
        }
        out.sort(Comparator.comparing(TransactionBriefDto::tradeTime, Comparator.nullsLast(String::compareTo)));
        return out;
    }

    private static String counterpartyKey(String c) {
        return (c == null || c.isBlank()) ? "" : c.trim();
    }

    private static boolean counterpartyMatches(String txCounterparty, String queryCounterparty) {
        String q = queryCounterparty == null ? "" : queryCounterparty.trim();
        String k = counterpartyKey(txCounterparty);
        if ("（未填写）".equals(q)) return k.isEmpty();
        return k.equals(q) || (txCounterparty != null && txCounterparty.trim().equals(q));
    }

    private static String laterTradeTime(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        var pa = TradeTimeUtil.parse(a);
        var pb = TradeTimeUtil.parse(b);
        if (pa.isEmpty()) return b;
        if (pb.isEmpty()) return a;
        return pa.get().isBefore(pb.get()) ? b : a;
    }

    private static List<WechatBillTransaction> filterInRange(
            List<WechatBillTransaction> txs, LocalDate from, LocalDate to) {
        List<WechatBillTransaction> inRange = new ArrayList<>();
        for (WechatBillTransaction t : txs) {
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) continue;
            LocalDate d = od.get();
            if (from != null && d.isBefore(from)) continue;
            if (to != null && d.isAfter(to)) continue;
            inRange.add(t);
        }
        return inRange;
    }

    private static final class CounterpartyAgg {
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        BigDecimal neutralTotal = BigDecimal.ZERO;
        int incomeCount;
        int expenseCount;
        int neutralCount;
        String lastTradeTime;

        void add(WechatBillTransaction t, IncomeExpenseKind k) {
            lastTradeTime = laterTradeTime(lastTradeTime, t.getTradeTime());
            switch (k) {
                case INCOME -> { incomeTotal = incomeTotal.add(t.getAmountYuan()); incomeCount++; }
                case EXPENSE -> { expenseTotal = expenseTotal.add(t.getAmountYuan()); expenseCount++; }
                case NEUTRAL -> { neutralTotal = neutralTotal.add(t.getAmountYuan()); neutralCount++; }
                default -> {}
            }
        }
    }
}
