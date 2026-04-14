package org.example.bill.config;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.domain.AppUserPhone;
import org.example.bill.domain.PhoneNumber;
import org.example.bill.mapper.PhoneNumberMapper;
import org.example.bill.repo.AppUserPhoneRepository;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.repo.RoleRepository;
import org.example.bill.util.AccountUsernameUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final AppUserRepository appUserRepository;
    private final AppUserPhoneRepository appUserPhoneRepository;
    private final RoleRepository roleRepository;
    private final PhoneNumberMapper phoneNumberMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Value("${app.bootstrap.admin-phone:}")
    private String adminPhone;

    @Bean
    ApplicationRunner seedAdmin() {
        return args -> {
            if (adminUsername == null
                    || adminUsername.isBlank()
                    || adminPassword == null
                    || adminPassword.isBlank()) {
                return;
            }
            String un = AccountUsernameUtil.normalize(adminUsername);
            if (!AccountUsernameUtil.isValid(un)) {
                return;
            }
            if (appUserRepository.existsByUsername(un)) {
                appUserRepository
                        .findByUsername(un)
                        .ifPresent(
                                u -> {
                                    if (u.getPasswordPlain() != null && !u.getPasswordPlain().isBlank()) {
                                        return;
                                    }
                                    if (passwordEncoder.matches(adminPassword, u.getPasswordHash())) {
                                        u.setPasswordPlain(adminPassword);
                                        appUserRepository.save(u);
                                    }
                                });
                return;
            }
            var adminRole =
                    roleRepository
                            .findByCode("ADMIN")
                            .orElseThrow(() -> new IllegalStateException("缺少 ADMIN 角色，请检查 Flyway"));
            AppUser u = new AppUser();
            u.setUsername(un);
            u.setPasswordHash(passwordEncoder.encode(adminPassword));
            u.setPasswordPlain(adminPassword);
            u.setEnabled(true);
            u.getRoles().add(adminRole);
            appUserRepository.save(u);

            // 绑定管理员手机号
            if (adminPhone != null && !adminPhone.isBlank()) {
                String mobile = adminPhone.trim();
                // 确保 phone_number 中有记录
                if (phoneNumberMapper.selectCount(null) == 0
                        || phoneNumberMapper.selectList(
                                        com.baomidou.mybatisplus.core.toolkit.Wrappers.<PhoneNumber>lambdaQuery()
                                                .eq(PhoneNumber::getMobileCn, mobile))
                                .isEmpty()) {
                    PhoneNumber pn = new PhoneNumber();
                    pn.setMobileCn(mobile);
                    pn.setCreatedAt(Instant.now());
                    phoneNumberMapper.insert(pn);
                }
                // 关联 app_user_phones
                if (appUserPhoneRepository.findByUserIdAndMobileCn(u.getId(), mobile).isEmpty()) {
                    AppUserPhone ap = new AppUserPhone();
                    ap.setUserId(u.getId());
                    ap.setMobileCn(mobile);
                    ap.setCreatedAt(Instant.now());
                    appUserPhoneRepository.save(ap);
                }
            }
        };
    }
}
