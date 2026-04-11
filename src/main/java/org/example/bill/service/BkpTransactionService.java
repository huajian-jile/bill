package org.example.bill.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.BillChannels;
import org.example.bill.domain.BkpWechatBillTransaction;
import org.example.bill.repo.BkpSpecifications;
import org.example.bill.repo.BkpWechatBillTransactionRepository;
import org.example.bill.web.dto.BkpSearchParams;
import org.example.bill.web.dto.BkpTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BkpTransactionService {

    private final BkpWechatBillTransactionRepository repo;

    public Page<BkpWechatBillTransaction> search(BkpSearchParams p) {
        Specification<BkpWechatBillTransaction> spec = BkpSpecifications.fromParams(p);
        String sf = p.sort() == null ? "tradeTime" : p.sort().trim();
        String sortField =
                switch (sf) {
                    case "tradeType", "counterparty", "incomeExpense", "amountYuan", "tradeTime" -> sf;
                    default -> "tradeTime";
                };
        Sort.Direction dir =
                "asc".equalsIgnoreCase(p.direction()) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(dir, sortField);
        return repo.findAll(spec, PageRequest.of(p.page(), p.size(), sort));
    }

    public BkpWechatBillTransaction get(Long id) {
        return repo.findById(id).orElseThrow();
    }

    @Transactional
    public BkpWechatBillTransaction create(BkpTransactionRequest req, Long appUserId) {
        BkpWechatBillTransaction e = new BkpWechatBillTransaction();
        apply(e, req);
        e.setAppUserId(appUserId);
        Instant n = Instant.now();
        e.setCreatedAt(n);
        e.setUpdatedAt(n);
        return repo.save(e);
    }

    @Transactional
    public BkpWechatBillTransaction update(Long id, BkpTransactionRequest req, Long appUserId) {
        BkpWechatBillTransaction e = get(id);
        apply(e, req);
        e.setUpdatedAt(Instant.now());
        e.setUpdatedBy(String.valueOf(appUserId));
        return repo.save(e);
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

    private void apply(BkpWechatBillTransaction e, BkpTransactionRequest r) {
        e.setSourceTxId(r.sourceTxId());
        e.setBillImportId(r.billImportId());
        e.setBillChannel(
                r.billChannel() != null && !r.billChannel().isBlank()
                        ? r.billChannel().trim().toUpperCase()
                        : BillChannels.WECHAT);
        e.setRowHash(r.rowHash());
        e.setTradeTime(r.tradeTime());
        e.setTradeType(r.tradeType());
        e.setCounterparty(r.counterparty());
        e.setProduct(r.product());
        e.setIncomeExpense(r.incomeExpense());
        e.setAmountYuan(r.amountYuan());
        e.setPaymentMethod(r.paymentMethod());
        e.setStatus(r.status());
        e.setTradeNo(r.tradeNo());
        e.setMerchantNo(r.merchantNo());
        e.setRemark(r.remark());
        e.setSourceFile(r.sourceFile());
        e.setExtraText(r.extraText());
        e.setArchived(r.archived() != null && r.archived());
    }
}
