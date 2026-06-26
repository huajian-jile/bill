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
    private final BillImportLinkageService billImportLinkageService;

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
        billImportLinkageService.ensurePhoneNumberRow(mobile);
    }

    public List<String> listMobiles(Long userId) {
        return appUserPhoneRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(AppUserPhone::getMobileCn)
                .toList();
    }

    /**
     * 写入一条账号与手机号的关联（多对多：同一账号可多条，同一号码也可被多账号关联）。
     * 业务上的审核/冲突策略由上层（如审核队列）负责；此处仅保证格式与同一账号下不重复插入。
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
        if (appUserPhoneRepository.findByUserIdAndMobileCn(userId, mobile).isPresent()) {
            billImportLinkageService.ensurePhoneNumberRow(mobile);
            return;
        }
        AppUserPhone row = new AppUserPhone();
        row.setUserId(userId);
        row.setMobileCn(mobile);
        appUserPhoneRepository.save(row);
        billImportLinkageService.ensurePhoneNumberRow(mobile);
    }

    @Transactional
    public void removePhone(Long userId, String rawMobile) {
        PhoneUtil.requireValidCnMobile(rawMobile);
        String mobile = PhoneUtil.normalizeCnMobile(rawMobile);
        AppUserPhone row =
                appUserPhoneRepository
                        .findByUserIdAndMobileCn(userId, mobile)
                        .orElseThrow(() -> new IllegalArgumentException("未找到该绑定号码"));
        appUserPhoneRepository.delete(row);
    }
}
