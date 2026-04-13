package org.example.bill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.domain.Permission;
import org.example.bill.domain.Role;
import org.example.bill.mapper.AppUserMapper;
import org.example.bill.mapper.PermissionMapper;
import org.example.bill.mapper.RoleMapper;
import org.example.bill.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RbacService {

    private final AppUserMapper appUserMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;

    public AppUser loadWithRolesAndPermissionsByUsername(String username) {
        AppUser u =
                appUserMapper.selectOne(
                        Wrappers.<AppUser>lambdaQuery().eq(AppUser::getUsername, username));
        if (u == null) {
            return null;
        }
        fillRolesAndPermissions(u);
        return u;
    }

    public AppUser loadWithRolesAndPermissionsById(Long id) {
        AppUser u = appUserMapper.selectById(id);
        if (u == null) {
            return null;
        }
        fillRolesAndPermissions(u);
        return u;
    }

    private void fillRolesAndPermissions(AppUser u) {
        List<Role> roles = roleMapper.selectRolesByUserId(u.getId());
        for (Role r : roles) {
            List<Permission> perms = permissionMapper.selectPermissionsByRoleId(r.getId());
            r.setPermissions(new HashSet<>(perms));
        }
        u.setRoles(new HashSet<>(roles));
    }

    @Transactional
    public void replaceUserRoles(Long userId, Set<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        for (Long rid : roleIds) {
            userRoleMapper.insert(userId, rid);
        }
    }

    public List<Role> resolveRolesByCodes(List<String> roleCodes) {
        return roleCodes.stream()
                .map(c -> roleMapper.selectByCode(c.trim().toUpperCase()))
                .filter(r -> r != null)
                .collect(Collectors.toList());
    }
}
