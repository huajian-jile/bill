package org.example.bill.config;

import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
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
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-username:}")
    private String adminUsername;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

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
                // 历史种子管理员未存档明文：若登录密码仍与 bootstrap 配置一致则补写（已改密则 hash 不匹配，不会覆盖）
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
        };
    }
}
