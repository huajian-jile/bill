package org.example.bill.web.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.domain.Role;
import org.example.bill.service.AdminUserService;
import org.example.bill.service.UserPhoneService;
import org.example.bill.web.dto.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_USER_ADMIN')")
public class UserAdminController {

    private final AdminUserService adminUserService;
    private final UserPhoneService userPhoneService;

    @GetMapping("/roles")
    public List<String> roleCodes() {
        return adminUserService.allRoleCodes();
    }

    @GetMapping
    public List<AdminUserView> list() {
        return adminUserService.listAll().stream().map(this::toView).toList();
    }

    @PostMapping
    public AdminUserView create(@RequestBody AdminUserCreateRequest req) {
        AppUser u =
                adminUserService.createUser(
                        req.username(), req.password(), req.roleCodes());
        return toView(u);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        adminUserService.deleteUser(id);
    }

    @PatchMapping("/{id}/password")
    public void password(@PathVariable Long id, @RequestBody AdminUserPasswordRequest req) {
        adminUserService.setPassword(id, req.password());
    }

    @DeleteMapping("/{id}/phones")
    public void unbindPhone(@PathVariable Long id, @RequestParam String mobile) {
        userPhoneService.removePhone(id, mobile);
    }

    @PutMapping("/{id}/roles")
    public void roles(@PathVariable Long id, @RequestBody AdminUserRolesRequest req) {
        adminUserService.replaceRoles(id, req.roleCodes());
    }

    @PatchMapping("/{id}/enabled")
    public void enabled(@PathVariable Long id, @RequestBody AdminUserEnabledRequest req) {
        adminUserService.setEnabled(id, req.enabled());
    }

    private AdminUserView toView(AppUser u) {
        List<String> roles = u.getRoles().stream().map(Role::getCode).toList();
        List<String> phones = userPhoneService.listMobiles(u.getId());
        return new AdminUserView(
                u.getId(), u.getUsername(), u.getPasswordPlain(), u.isEnabled(), roles, phones);
    }
}
