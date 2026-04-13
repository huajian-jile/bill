package org.example.bill.util;

import java.util.regex.Pattern;

/** 登录账号：10 位数字（不以手机号作为用户名）。 */
public final class AccountUsernameUtil {

    private static final Pattern TEN_DIGITS = Pattern.compile("^[0-9]{10}$");

    private AccountUsernameUtil() {}

    public static boolean isValid(String s) {
        if (s == null) {
            return false;
        }
        return TEN_DIGITS.matcher(s.trim()).matches();
    }

    public static String normalize(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }

    public static void requireValid(String username) {
        String n = normalize(username);
        if (!isValid(n)) {
            throw new IllegalArgumentException("账号须为 10 位数字");
        }
    }
}
