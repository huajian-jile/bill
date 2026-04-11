package org.example.bill.util;

import java.util.regex.Pattern;

public final class PhoneUtil {

    private static final Pattern CN_MOBILE = Pattern.compile("^1[3-9]\\d{9}$");

    private PhoneUtil() {}

    public static boolean isValidCnMobile(String s) {
        if (s == null) {
            return false;
        }
        String t = s.trim();
        return CN_MOBILE.matcher(t).matches();
    }

    public static String normalizeCnMobile(String s) {
        if (s == null) {
            return null;
        }
        return s.trim();
    }

    public static void requireValidCnMobile(String mobileCn) {
        String n = normalizeCnMobile(mobileCn);
        if (!isValidCnMobile(n)) {
            throw new IllegalArgumentException("手机号须为 11 位中国大陆号码（1 开头）");
        }
    }
}
