package org.example.bill.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.bill.domain.AppUser;
import org.example.bill.domain.WxPhoneSession;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.repo.RoleRepository;
import org.example.bill.repo.WxPhoneSessionRepository;
import org.example.bill.util.PhoneUtil;
import org.example.bill.web.AuthResponseMapper;
import org.example.bill.web.dto.LoginResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
@Slf4j
public class WxAuthService {

    @Value("${app.wx.app-id}")
    private String appId;

    @Value("${app.wx.app-secret}")
    private String appSecret;

    private final WxPhoneSessionRepository sessionRepo;
    private final AppUserRepository appUserRepo;
    private final RoleRepository roleRepo;
    private final AuthResponseMapper authResponseMapper;
    private final UserPhoneService userPhoneService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    /** 用 code 换 session_key（有效期5分钟，一次性） */
    @Transactional
    public String exchangeCodeForSessionKey(String code) {
        // 先删过期记录
        sessionRepo.deleteExpired(Instant.now());

        String url = String.format(
            "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
            appId, appSecret, code);

        String resp = restTemplate.getForObject(url, String.class);
        JsonNode node;
        try {
            node = objectMapper.readTree(resp);
        } catch (Exception e) {
            throw new RuntimeException("微信接口返回格式错误: " + resp);
        }

        if (node.has("errcode") && node.get("errcode").asInt() != 0) {
            throw new RuntimeException("微信 code 错误: " + node.get("errmsg").asText());
        }

        String sessionKey = node.get("session_key").asText();
        String openid = node.has("openid") ? node.get("openid").asText() : null;

        // 存 session_key，5分钟过期
        WxPhoneSession sess = new WxPhoneSession(
            code, sessionKey, openid, Instant.now().plus(5, ChronoUnit.MINUTES));
        sessionRepo.save(sess);

        return sessionKey;
    }

    /** 用 session_key 解密手机号 */
    public String decryptPhoneNumber(String sessionKey, String encryptedData, String iv) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(sessionKey);
            byte[] ivBytes = Base64.getDecoder().decode(iv);
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(encryptedBytes);
            String json = new String(decrypted, "UTF-8");
            JsonNode node = objectMapper.readTree(json);
            String phoneNumber = node.get("phoneNumber").asText();
            PhoneUtil.requireValidCnMobile(phoneNumber);
            return phoneNumber;
        } catch (Exception e) {
            throw new RuntimeException("手机号解密失败: " + e.getMessage(), e);
        }
    }

    /** 用 code + encryptedData + iv 完成登录/注册，返回 LoginResponse */
    @Transactional
    public LoginResponse loginWithWxCode(String code, String encryptedData, String iv) {
        // 1. 用 code 换 session_key
        String sessionKey = exchangeCodeForSessionKey(code);

        // 2. 解密手机号
        String mobile = decryptPhoneNumber(sessionKey, encryptedData, iv);

        // 3. 用完删除 session（一次性）
        sessionRepo.findByCode(code).ifPresent(sessionRepo::delete);

        // 4. 查找或创建 AppUser（手机号即用户名）
        AppUser u = appUserRepo.findByUsername(mobile).orElseGet(() -> {
            var viewer = roleRepo.findByCode("USER")
                .orElseThrow(() -> new IllegalStateException("缺少 USER 角色"));
            AppUser nu = new AppUser();
            nu.setUsername(mobile);
            nu.setPasswordHash(""); // 微信登录不需要密码
            nu.setPasswordPlain(null);
            nu.setEnabled(true);
            nu.getRoles().add(viewer);
            appUserRepo.save(nu);
            // 自动绑定手机号
            userPhoneService.addPhone(nu.getId(), mobile);
            return nu;
        });

        return authResponseMapper.toLoginResponse(u);
    }
}
