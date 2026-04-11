package org.example.bill.config;

import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.repo.RoleRepository;
import org.example.bill.util.PhoneUtil;
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

    @Value("${app.bootstrap.admin-mobile:}")
    private String adminMobile;

    @Value("${app.bootstrap.admin-password:}")
    private String adminPassword;

    @Bean
    ApplicationRunner seedAdmin() {
        return args -> {
            if (adminMobile == null || adminMobile.isBlank() || adminPassword == null || adminPassword.isBlank()) {
                return;
            }
            String mobile = PhoneUtil.normalizeCnMobile(adminMobile);
            if (!PhoneUtil.isValidCnMobile(mobile)) {
                return;
            }
            if (appUserRepository.existsByUsername(mobile)) {
                return;
            }
            var adminRole =
                    roleRepository
                            .findByCode("ADMIN")
                            .orElseThrow(() -> new IllegalStateException("缺少 ADMIN 角色，请检查 Flyway"));
            AppUser u = new AppUser();
            u.setUsername(mobile);
            u.setPasswordHash(passwordEncoder.encode(adminPassword));
            u.setEnabled(true);
            u.getRoles().add(adminRole);
            appUserRepository.save(u);
        };
    }
}
