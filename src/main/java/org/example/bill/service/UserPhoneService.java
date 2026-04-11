package org.example.bill.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.domain.AppUserPhone;
import org.example.bill.repo.AppUserPhoneRepository;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.util.PhoneUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPhoneService {

    private final AppUserPhoneRepository appUserPhoneRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public void ensureLoginPhoneBound(AppUser user) {
        String mobile = PhoneUtil.normalizeCnMobile(user.getUsername());
        if (!PhoneUtil.isValidCnMobile(mobile)) {
            return;
        }
        if (appUserPhoneRepository.findByUserIdAndMobileCn(user.getId(), mobile).isEmpty()) {
            AppUserPhone row = new AppUserPhone();
            row.setUserId(user.getId());
            row.setMobileCn(mobile);
            appUserPhoneRepository.save(row);
        }
    }

    public List<String> listMobiles(Long userId) {
        return appUserPhoneRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(AppUserPhone::getMobileCn)
                .toList();
    }

    /**
     * 绑定额外手机号：全局唯一（不能与任一账号登录手机号或其它已绑定号重复）。
     */
    @Transactional
    public void addPhone(Long userId, String rawMobile) {
        PhoneUtil.requireValidCnMobile(rawMobile);
        String mobile = PhoneUtil.normalizeCnMobile(rawMobile);
        AppUser self = appUserRepository.findById(userId).orElseThrow();
        if (mobile.equals(PhoneUtil.normalizeCnMobile(self.getUsername()))) {
            ensureLoginPhoneBound(self);
            return;
        }
        if (appUserRepository.existsByUsername(mobile)) {
            throw new IllegalArgumentException("该手机号已被用作登录账号");
        }
        if (appUserPhoneRepository.existsByMobileCn(mobile)) {
            throw new IllegalArgumentException("该手机号已被绑定");
        }
        AppUserPhone row = new AppUserPhone();
        row.setUserId(userId);
        row.setMobileCn(mobile);
        appUserPhoneRepository.save(row);
    }
}
