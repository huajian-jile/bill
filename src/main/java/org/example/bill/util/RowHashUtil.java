package org.example.bill.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RowHashUtil {

    private RowHashUtil() {}

    public static String hash(
            String tradeTime,
            String tradeNo,
            String merchantNo,
            BigDecimal amount,
            String tradeType,
            String counterparty) {
        String s =
                String.join(
                        "|",
                        nz(tradeTime),
                        nz(tradeNo),
                        nz(merchantNo),
                        amount == null ? "" : amount.stripTrailingZeros().toPlainString(),
                        nz(tradeType),
                        nz(counterparty));
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
