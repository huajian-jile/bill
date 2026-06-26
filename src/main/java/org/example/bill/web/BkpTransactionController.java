package org.example.bill.web;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.BkpWechatBillTransaction;
import org.example.bill.service.BkpTransactionService;
import org.example.bill.web.dto.BkpSearchParams;
import org.example.bill.web.dto.BkpTransactionRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bkp/transactions")
@RequiredArgsConstructor
public class BkpTransactionController {

    private final BkpTransactionService service;
    private final SecurityUtil securityUtil;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Page<BkpWechatBillTransaction> search(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) Long billImportId,
            @RequestParam(required = false) String tradeTimeFrom,
            @RequestParam(required = false) String tradeTimeTo,
            @RequestParam(required = false) String tradeType,
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String incomeExpense,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "tradeTime") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return service.search(
                new BkpSearchParams(
                        channel,
                        billImportId,
                        tradeTimeFrom,
                        tradeTimeTo,
                        tradeType,
                        counterparty,
                        incomeExpense,
                        amountMin,
                        amountMax,
                        page,
                        size,
                        sort,
                        direction));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public BkpWechatBillTransaction get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public BkpWechatBillTransaction create(@Valid @RequestBody BkpTransactionRequest req) {
        return service.create(req, securityUtil.currentUserId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public BkpWechatBillTransaction update(
            @PathVariable Long id, @Valid @RequestBody BkpTransactionRequest req) {
        return service.update(id, req, securityUtil.currentUserId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
