package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.WechatBillImport;
import org.example.bill.repo.WechatBillImportRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wechat-imports")
@RequiredArgsConstructor
public class WechatBillImportQueryController {

    private final WechatBillImportRepository repo;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public List<WechatBillImport> list(@RequestParam(required = false) Long wechatUserId) {
        if (wechatUserId != null) {
            return repo.findByUserId(wechatUserId);
        }
        return repo.findAll();
    }
}
