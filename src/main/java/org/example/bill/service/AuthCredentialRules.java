package org.example.bill.service;

import org.springframework.stereotype.Component;

@Component
public class AuthCredentialRules {

    public static final int PASSWORD_MAX_LEN = 128;

    public void requirePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (password.length() > PASSWORD_MAX_LEN) {
            throw new IllegalArgumentException("密码过长（最多 " + PASSWORD_MAX_LEN + " 字符）");
        }
    }
}
