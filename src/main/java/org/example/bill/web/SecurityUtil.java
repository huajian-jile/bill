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

    /** 是否具备“查看全量账单数据”权限（admin/master）。 */
    public boolean currentUserIsAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "PERM_VIEW_ALL_BILLS".equals(a.getAuthority()));
    }
}
