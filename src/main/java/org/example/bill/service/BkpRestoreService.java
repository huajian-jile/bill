package org.example.bill.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AlipayBillTransaction;
import org.example.bill.domain.BillChannels;
import org.example.bill.domain.BkpWechatBillTransaction;
import org.example.bill.domain.WechatBillTransaction;
import org.example.bill.repo.AlipayBillTransactionRepository;
import org.example.bill.repo.BkpWechatBillTransactionRepository;
import org.example.bill.repo.WechatBillTransactionRepository;
import org.example.bill.util.TradeTimeUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BkpRestoreService {

    private final WechatBillTransactionRepository wechatRepo;
    private final AlipayBillTransactionRepository alipayRepo;
    private final BkpWechatBillTransactionRepository bkpRepo;

    @Transactional
    public int restoreWechatDay(LocalDate day, Long appUserId) {
        return restoreWechatRange(day, day, appUserId);
    }

    @Transactional
    public int restoreWechatMonth(int year, int month, Long appUserId) {
        YearMonth ym = YearMonth.of(year, month);
        return restoreWechatRange(ym.atDay(1), ym.atEndOfMonth(), appUserId);
    }

    @Transactional
    public int restoreWechatYear(int year, Long appUserId) {
        return restoreWechatRange(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), appUserId);
    }

    @Transactional
    public int restoreWechatAll(Long appUserId) {
        int n = 0;
        for (WechatBillTransaction t : wechatRepo.findAll()) {
            if (t.isArchived()) {
                continue;
            }
            upsertFromWechat(t, appUserId);
            n++;
        }
        return n;
    }

    private int restoreWechatRange(LocalDate from, LocalDate to, Long appUserId) {
        int n = 0;
        for (WechatBillTransaction t : wechatRepo.findAll()) {
            if (t.isArchived()) {
                continue;
            }
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) {
                continue;
            }
            LocalDate d = od.get();
            if (d.isBefore(from) || d.isAfter(to)) {
                continue;
            }
            upsertFromWechat(t, appUserId);
            n++;
        }
        return n;
    }

    private void upsertFromWechat(WechatBillTransaction t, Long appUserId) {
        BkpWechatBillTransaction e =
                bkpRepo
                        .findByBillChannelAndSourceTxId(BillChannels.WECHAT, t.getId())
                        .orElseGet(BkpWechatBillTransaction::new);
        copyWechatToBkp(t, e, appUserId);
        bkpRepo.save(e);
    }

    private void copyWechatToBkp(WechatBillTransaction t, BkpWechatBillTransaction e, Long appUserId) {
        e.setBillChannel(BillChannels.WECHAT);
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
        e.setArchived(t.isArchived());
        e.setAppUserId(appUserId);
        Instant now = Instant.now();
        if (e.getId() == null) {
            e.setCreatedAt(now);
        }
        e.setUpdatedAt(now);
    }

    @Transactional
    public int restoreAlipayDay(LocalDate day, Long appUserId) {
        return restoreAlipayRange(day, day, appUserId);
    }

    @Transactional
    public int restoreAlipayMonth(int year, int month, Long appUserId) {
        YearMonth ym = YearMonth.of(year, month);
        return restoreAlipayRange(ym.atDay(1), ym.atEndOfMonth(), appUserId);
    }

    @Transactional
    public int restoreAlipayYear(int year, Long appUserId) {
        return restoreAlipayRange(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), appUserId);
    }

    @Transactional
    public int restoreAlipayAll(Long appUserId) {
        int n = 0;
        for (AlipayBillTransaction t : alipayRepo.findAll()) {
            if (t.isArchived()) {
                continue;
            }
            upsertFromAlipay(t, appUserId);
            n++;
        }
        return n;
    }

    private int restoreAlipayRange(LocalDate from, LocalDate to, Long appUserId) {
        int n = 0;
        for (AlipayBillTransaction t : alipayRepo.findAll()) {
            if (t.isArchived()) {
                continue;
            }
            var od = TradeTimeUtil.parseDate(t.getTradeTime());
            if (od.isEmpty()) {
                continue;
            }
            LocalDate d = od.get();
            if (d.isBefore(from) || d.isAfter(to)) {
                continue;
            }
            upsertFromAlipay(t, appUserId);
            n++;
        }
        return n;
    }

    private void upsertFromAlipay(AlipayBillTransaction t, Long appUserId) {
        BkpWechatBillTransaction e =
                bkpRepo
                        .findByBillChannelAndSourceTxId(BillChannels.ALIPAY, t.getId())
                        .orElseGet(BkpWechatBillTransaction::new);
        copyAlipayToBkp(t, e, appUserId);
        bkpRepo.save(e);
    }

    private void copyAlipayToBkp(AlipayBillTransaction t, BkpWechatBillTransaction e, Long appUserId) {
        e.setBillChannel(BillChannels.ALIPAY);
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
        e.setArchived(t.isArchived());
        e.setAppUserId(appUserId);
        Instant now = Instant.now();
        if (e.getId() == null) {
            e.setCreatedAt(now);
        }
        e.setUpdatedAt(now);
    }

    private static String trimHash(String h) {
        return h == null ? null : h.trim();
    }
}
