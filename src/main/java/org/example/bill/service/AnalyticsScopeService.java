package org.example.bill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.PhoneNumber;
import org.example.bill.domain.WechatUser;
import org.example.bill.mapper.AlipayUserMapper;
import org.example.bill.mapper.PhoneNumberMapper;
import org.example.bill.mapper.WechatUserMapper;
import org.example.bill.web.SecurityUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 分析接口：将前端传来的 phoneId / phoneIds（phone_number.id）解析为 wechat_users.id 或 alipay_users.id 列表；
 * 非管理员仅能查本人已绑定号码对应的数据。
 */
@Service
@RequiredArgsConstructor
public class AnalyticsScopeService {

    private final SecurityUtil securityUtil;
    private final UserBillPhoneService userBillPhoneService;
    private final UserPhoneService userPhoneService;
    private final WechatUserMapper wechatUserMapper;
    private final AlipayUserMapper alipayUserMapper;
    private final PhoneNumberMapper phoneNumberMapper;

    /**
     * @return null 表示不限制微信用户（仅管理员、且未指定号码时）；非空列表为限定范围；空列表表示无可见用户（结果应为空）
     */
    public List<Long> resolveWechatUserIds(boolean admin, Long phoneId, String phoneIdsCsv) {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        Set<Long> allowedPhones = userBillPhoneService.allowedPhoneIds(uid);
        Set<Long> selectedPhones = new LinkedHashSet<>();
        if (phoneIdsCsv != null && !phoneIdsCsv.isBlank()) {
            for (String part : phoneIdsCsv.split(",")) {
                String s = part.trim();
                if (s.isEmpty()) {
                    continue;
                }
                long id = Long.parseLong(s);
                if (!admin && !allowedPhones.contains(id)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该号码筛选");
                }
                selectedPhones.add(id);
            }
        } else if (phoneId != null) {
            if (!admin && !allowedPhones.contains(phoneId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该号码筛选");
            }
            selectedPhones.add(phoneId);
        } else {
            if (admin) {
                return null;
            }
            selectedPhones.addAll(allowedPhones);
        }
        if (selectedPhones.isEmpty()) {
            return List.of();
        }
        List<WechatUser> wus =
                wechatUserMapper.selectList(
                        Wrappers.<WechatUser>lambdaQuery()
                                .in(WechatUser::getPhoneId, selectedPhones));
        List<Long> out =
                wus.stream().map(WechatUser::getId).distinct().sorted().toList();
        return new ArrayList<>(out);
    }

    /**
     * 解析 Alipay 用户范围：
     * - 管理员 + 未指定号码 → null（全量）
     * - 非管理员 + 未指定号码 → 仅本人绑定号码对应的 alipay_users.id
     * - 指定号码 → 验证权限后返回对应的 alipay_users.id
     * @return null 表示不限制；非空列表（含空列表）为限定范围
     */
    public List<Long> resolveAlipayUserIds(boolean admin, Long phoneId, String phoneIdsCsv) {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        // 非管理员：只能查本人绑定号码对应的 alipay_users
        List<String> allowedMobiles = userPhoneService.listMobiles(uid);
        Set<String> allowedMobileSet = new LinkedHashSet<>(allowedMobiles);

        Set<String> selectedMobiles = new LinkedHashSet<>();
        if (phoneIdsCsv != null && !phoneIdsCsv.isBlank()) {
            // 指定了 phoneIds → 转为 mobileCn 后验证权限
            for (String part : phoneIdsCsv.split(",")) {
                String s = part.trim();
                if (s.isEmpty()) {
                    continue;
                }
                Long pid = Long.parseLong(s);
                PhoneNumber pn = phoneNumberMapper.selectById(pid);
                if (pn == null || pn.getMobileCn() == null) {
                    continue;
                }
                String mobile = pn.getMobileCn();
                if (!admin && !allowedMobileSet.contains(mobile)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该号码筛选");
                }
                selectedMobiles.add(mobile);
            }
        } else if (phoneId != null) {
            PhoneNumber pn = phoneNumberMapper.selectById(phoneId);
            if (pn != null && pn.getMobileCn() != null) {
                String mobile = pn.getMobileCn();
                if (!admin && !allowedMobileSet.contains(mobile)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权使用该号码筛选");
                }
                selectedMobiles.add(mobile);
            }
        } else {
            if (admin) {
                return null;
            }
            selectedMobiles.addAll(allowedMobileSet);
        }

        if (selectedMobiles.isEmpty()) {
            return List.of();
        }
        // 用 mobileCn 找 alipay_users.id
        List<Long> alipayIds = alipayUserMapper.findIdsByMobileCns(selectedMobiles);
        return alipayIds.stream().distinct().sorted().toList();
    }
}
