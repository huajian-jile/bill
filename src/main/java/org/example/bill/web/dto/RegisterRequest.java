package org.example.bill.web.dto;

/** mobile 可选：有则校验为大陆 11 位并写入绑定表 */
public record RegisterRequest(String username, String password, String mobile) {}
