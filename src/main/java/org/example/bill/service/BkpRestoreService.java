package org.example.bill.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.BillChannels;
import org.example.bill.domain.BkpWechatBillTransaction;
import org.example.bill.domain.WechatBillTransaction;
import org.example.bill.repo.BkpWechatBillTransactionRepository;
import org.example.bill.repo.WechatBillTransactionRepository;
import org.example.bill.util.TradeTimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BkpRestoreService {

    private final WechatBillTransactionRepository txRepo;
    private final BkpWechatBillTransactionRepository bkpRepo;

    @Transactional
    public int restoreWechatDay(LocalDate day, Long appUserId) {
        return restoreByChannel(BillChannels.WECHAT, day, day, appUserId);
    }

    @Transactional
    public int restoreWechatMonth(int year, int month, Long appUserId) {
        YearMonth ym = YearMonth.of(year, month);
        return restoreByChannel(BillChannels.WECHAT, ym.atDay(1), ym.atEndOfMonth(), appUserId);
    }

    @Transactional
    public int restoreWechatYear(int year, Long appUserId) {
        return restoreByChannel(BillChannels.WECHAT,
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), appUserId);
    }

    @Transactional
    public int restoreWechatAll(Long appUserId) {
        return restoreByChannel(BillChannels.WECHAT, null, null, appUserId);
    }

    @Transactional
    public int restoreAlipayDay(LocalDate day, Long appUserId) {
        return restoreByChannel(BillChannels.ALIPAY, day, day, appUserId);
    }

    @Transactional
    public int restoreAlipayMonth(int year, int month, Long appUserId) {
        YearMonth ym = YearMonth.of(year, month);
        return restoreByChannel(BillChannels.ALIPAY, ym.atDay(1), ym.atEndOfMonth(), appUserId);
    }

    @Transactional
    public int restoreAlipayYear(int year, Long appUserId) {
        return restoreByChannel(BillChannels.ALIPAY,
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), appUserId);
    }

    @Transactional
    public int restoreAlipayAll(Long appUserId) {
        return restoreByChannel(BillChannels.ALIPAY, null, null, appUserId);
    }

    private int restoreByChannel(String channel, LocalDate from, LocalDate to, Long appUserId) {
        int n = 0;
        for (WechatBillTransaction t : txRepo.findAll()) {
            if (!channel.equals(t.getChannel())) continue;
            if (Boolean.TRUE.equals(t.getArchived())) continue;
            if (from != null || to != null) {
                var od = TradeTimeUtil.parseDate(t.getTradeTime());
                if (od.isEmpty()) continue;
                LocalDate d = od.get();
                if (from != null && d.isBefore(from)) continue;
                if (to != null && d.isAfter(to)) continue;
            }
            upsertToBkp(t, channel, appUserId);
            n++;
        }
        return n;
    }

    private void upsertToBkp(WechatBillTransaction t, String channel, Long appUserId) {
        BkpWechatBillTransaction e =
                bkpRepo.findByBillChannelAndSourceTxId(channel, t.getId())
                        .orElseGet(BkpWechatBillTransaction::new);
        e.setBillChannel(channel);
        e.setSourceTxId(t.getId());
        e.setBillImportId(t.getBillImportId());
        e.setRowHash(trimHash(t.getRowHash()));
        e.setTradeTime(t.getTradeTime());
        e.setTradeType(t.getTradeType());
        e.setCounterparty(t.getCounterparty());
        e.setProduct(t.getProduct());
        e.setIncomeExpense(t.getIncomeExpense());
        e.setAmountYuan(t.getAmountYuan());
        e.setPaymentMethod(t.getPaymentMethod());
        e.setStatus(t.getStatus());
        e.setTradeNo(t.getTradeNo());
        e.setMerchantNo(t.getMerchantNo());
        e.setRemark(t.getRemark());
        e.setSourceFile(t.getSourceFile());
        e.setExtraText(t.getExtraText());
        e.setArchived(t.getArchived());
        e.setAppUserId(appUserId);
        Instant now = Instant.now();
        if (e.getId() == null) e.setCreatedAt(now);
        e.setUpdatedAt(now);
        bkpRepo.save(e);
    }

    private static String trimHash(String h) {
        return h == null ? null : h.trim();
    }
}
