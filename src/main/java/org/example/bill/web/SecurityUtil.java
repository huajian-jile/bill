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

    /** 是否具备用户管理权限（与路由里 PERM_USER_ADMIN 一致）。 */
    public boolean currentUserIsAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "PERM_USER_ADMIN".equals(a.getAuthority()));
    }
}
