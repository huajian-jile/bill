package org.example.bill.service;

public enum AnalyticsChannel {
    wechat,
    alipay,
    merged;

    public static AnalyticsChannel fromParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return wechat;
        }
        return switch (raw.trim().toLowerCase()) {
            case "alipay" -> alipay;
            case "merged" -> merged;
            default -> wechat;
        };
    }
}
