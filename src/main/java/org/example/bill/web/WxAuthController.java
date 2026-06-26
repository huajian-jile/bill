package org.example.bill.web;

import lombok.RequiredArgsConstructor;
import org.example.bill.service.WxAuthService;
import org.example.bill.web.dto.LoginResponse;
import org.example.bill.web.dto.WxLoginRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wx")
@RequiredArgsConstructor
public class WxAuthController {

    private final WxAuthService wxAuthService;

    /**
     * 微信手机号授权登录
     * 请求体: { code: wx.login()获取的code, encryptedData: getPhoneNumber加密数据, iv: 加密iv }
     * 返回: LoginResponse (含JWT)
     */
    @PostMapping("/login")
    public LoginResponse wxLogin(@RequestBody WxLoginRequest req) {
        return wxAuthService.loginWithWxCode(req.code(), req.encryptedData(), req.iv());
    }
}
