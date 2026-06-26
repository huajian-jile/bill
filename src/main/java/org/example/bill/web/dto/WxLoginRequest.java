package org.example.bill.web.dto;

public record WxLoginRequest(String code, String encryptedData, String iv) {}
