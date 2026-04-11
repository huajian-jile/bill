package org.example.bill.web;

import lombok.RequiredArgsConstructor;
import org.example.bill.repo.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final AppUserRepository appUserRepository;

    public Long currentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return appUserRepository.findByUsername(auth.getName()).map(u -> u.getId()).orElse(null);
    }
}
