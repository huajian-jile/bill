package org.example.bill.web;

import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.repo.RoleRepository;
import org.example.bill.service.UserPhoneService;
import org.example.bill.util.PhoneUtil;
import org.example.bill.web.dto.LoginRequest;
import org.example.bill.web.dto.LoginResponse;
import org.example.bill.web.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserPhoneService userPhoneService;
    private final AuthResponseMapper authResponseMapper;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest req) {
        // 校验密码非空
        String pwd = req.password();
        if (pwd == null || pwd.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (pwd.length() > 128) {
            throw new IllegalArgumentException("密码不能超过 128 字符");
        }
        // 校验确认密码
        if (!pwd.equals(req.confirmPassword())) {
            throw new IllegalArgumentException("两次输入的密码不一致");
        }
        // 校验手机号
        String mobile = PhoneUtil.normalizeCnMobile(req.mobile());
        PhoneUtil.requireValidCnMobile(mobile);
        // 账号用手机号
        if (appUserRepository.existsByUsername(mobile)) {
            throw new IllegalArgumentException("该手机号已注册，请直接登录");
        }
        var viewer =
                roleRepository
                        .findByCode("VIEWER")
                        .orElseThrow(() -> new IllegalStateException("缺少 VIEWER 角色，请检查 Flyway"));
        AppUser u = new AppUser();
        u.setUsername(mobile);
        u.setPasswordHash(passwordEncoder.encode(pwd));
        u.setPasswordPlain(pwd);
        u.setEnabled(true);
        u.getRoles().add(viewer);
        appUserRepository.save(u);

        // 注册时自动绑定手机号
        userPhoneService.addPhone(u.getId(), mobile);

        AppUser fresh =
                appUserRepository
                        .findByUsername(mobile)
                        .orElseThrow(() -> new IllegalStateException("注册后加载用户失败"));
        return ResponseEntity.ok(authResponseMapper.toLoginResponse(fresh));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        String identifier = req.mobile() == null ? "" : req.mobile().trim();
        String pwd = req.password();
        if (pwd == null || pwd.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        String username;
        if (PhoneUtil.isValidCnMobile(identifier)) {
            // 11位手机号 → 直接作为用户名认证
            username = PhoneUtil.normalizeCnMobile(identifier);
        } else {
            // 非手机号格式（旧账号，10位数字用户名）→ 直接作为用户名
            username = identifier;
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, pwd));
        AppUser u =
                appUserRepository
                        .findByUsername(username)
                        .orElseThrow(() -> new IllegalStateException("用户不存在"));
        userPhoneService.ensureLoginPhoneBound(u);
        return ResponseEntity.ok(authResponseMapper.toLoginResponse(u));
    }
}
