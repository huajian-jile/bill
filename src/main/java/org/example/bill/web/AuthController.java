package org.example.bill.web;

import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.repo.RoleRepository;
import org.example.bill.service.AuthCredentialRules;
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
    private final AuthCredentialRules authCredentialRules;
    private final AuthResponseMapper authResponseMapper;

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@RequestBody RegisterRequest req) {
        authCredentialRules.requirePassword(req.password());
        PhoneUtil.requireValidCnMobile(req.mobile());
        String mobile = PhoneUtil.normalizeCnMobile(req.mobile());
        if (appUserRepository.existsByUsername(mobile)) {
            throw new IllegalArgumentException("该手机号已注册，请直接登录");
        }
        var viewer =
                roleRepository
                        .findByCode("VIEWER")
                        .orElseThrow(() -> new IllegalStateException("缺少 VIEWER 角色，请检查 Flyway"));
        AppUser u = new AppUser();
        u.setUsername(mobile);
        u.setPasswordHash(passwordEncoder.encode(req.password()));
        u.setEnabled(true);
        u.getRoles().add(viewer);
        appUserRepository.save(u);
        userPhoneService.ensureLoginPhoneBound(u);
        AppUser fresh =
                appUserRepository
                        .findByUsername(mobile)
                        .orElseThrow(() -> new IllegalStateException("注册后加载用户失败"));
        return ResponseEntity.ok(authResponseMapper.toLoginResponse(fresh));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest req) {
        PhoneUtil.requireValidCnMobile(req.username());
        authCredentialRules.requirePassword(req.password());
        String mobile = PhoneUtil.normalizeCnMobile(req.username());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(mobile, req.password()));
        AppUser u =
                appUserRepository
                        .findByUsername(mobile)
                        .orElseThrow(() -> new IllegalStateException("用户不存在"));
        userPhoneService.ensureLoginPhoneBound(u);
        return ResponseEntity.ok(authResponseMapper.toLoginResponse(u));
    }
}
