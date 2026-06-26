package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.WechatBillTransaction;
import org.example.bill.repo.WechatBillTransactionRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 只读：原始导入表 wechat_bill_transactions，供对账展示，不可修改 */
@RestController
@RequestMapping("/api/original/transactions")
@RequiredArgsConstructor
public class OriginalTransactionReadController {

    private final WechatBillTransactionRepository repo;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_VIEW_ALL_BILLS')")
    public List<WechatBillTransaction> list(@RequestParam(required = false) Long wechatUserId) {
        if (wechatUserId == null) {
            return repo.findAll();
        }
        return repo.findAllByWechatUserId(wechatUserId);
    }
}
