package org.example.bill.web.dto;

/** mobile 为账号，password 与 confirmPassword 须一致 */
public record RegisterRequest(String mobile, String password, String confirmPassword) {}
