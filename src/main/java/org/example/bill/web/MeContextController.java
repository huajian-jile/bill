package org.example.bill.web;

import lombok.RequiredArgsConstructor;
import org.example.bill.repo.WechatBillImportRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录账号（手机号）在导入账单时关联的 wechat_users.id，用于分析页默认筛选「自己的」流水。
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeContextController {

    private final WechatBillImportRepository wechatBillImportRepository;

    @GetMapping("/linked-wechat-user-id")
    public Long linkedWechatUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String mobile = auth.getName();
        if (mobile == null || mobile.isBlank()) {
            return null;
        }
        return wechatBillImportRepository
                .findTopByMobileCnOrderByIdAsc(mobile.trim())
                .map(org.example.bill.domain.WechatBillImport::getUserId)
                .orElse(null);
    }
}
