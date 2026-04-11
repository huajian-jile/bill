package org.example.bill.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.domain.Role;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.repo.RoleRepository;
import org.example.bill.util.PhoneUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPhoneService userPhoneService;
    private final AuthCredentialRules authCredentialRules;

    public List<AppUser> listAll() {
        return appUserRepository.findAll();
    }

    @Transactional
    public AppUser createUser(String mobileRaw, String password, List<String> roleCodes) {
        PhoneUtil.requireValidCnMobile(mobileRaw);
        authCredentialRules.requirePassword(password);
        String mobile = PhoneUtil.normalizeCnMobile(mobileRaw);
        if (appUserRepository.existsByUsername(mobile)) {
            throw new IllegalArgumentException("该手机号已存在");
        }
        Set<Role> roles = resolveRoles(roleCodes);
        AppUser u = new AppUser();
        u.setUsername(mobile);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setEnabled(true);
        u.setRoles(roles);
        appUserRepository.save(u);
        userPhoneService.ensureLoginPhoneBound(u);
        return u;
    }

    @Transactional
    public void replaceRoles(Long userId, List<String> roleCodes) {
        AppUser u =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        u.setRoles(resolveRoles(roleCodes));
    }

    @Transactional
    public void setEnabled(Long userId, boolean enabled) {
        AppUser u =
                appUserRepository
                        .findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        u.setEnabled(enabled);
    }

    private Set<Role> resolveRoles(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new IllegalArgumentException("至少选择一个角色");
        }
        Set<Role> set = new HashSet<>();
        for (String code : roleCodes) {
            Role r =
                    roleRepository
                            .findByCode(code.trim().toUpperCase())
                            .orElseThrow(() -> new IllegalArgumentException("未知角色: " + code));
            set.add(r);
        }
        return set;
    }

    public List<String> allRoleCodes() {
        return roleRepository.findAll().stream().map(Role::getCode).toList();
    }
}
