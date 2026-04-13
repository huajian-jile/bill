package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.repo.WechatBillImportRepository;
import org.example.bill.service.UserBillPhoneService;
import org.example.bill.web.dto.PhoneOptionDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 当前登录账号（手机号）在导入账单时关联的 wechat_users.id，用于分析页默认筛选「自己的」流水。
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeContextController {

    private final WechatBillImportRepository wechatBillImportRepository;
    private final SecurityUtil securityUtil;
    private final UserBillPhoneService userBillPhoneService;

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

    /**
     * 分析/分组看板下拉：普通用户为已绑定号码；管理员为系统中全部 phone_number（便于查看所有人数据）。
     */
    @GetMapping("/bill-phones")
    public List<PhoneOptionDto> billPhones() {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (securityUtil.currentUserIsAdmin()) {
            return userBillPhoneService.listAllPhoneOptions();
        }
        return userBillPhoneService.listOptions(uid);
    }
}
