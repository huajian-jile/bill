package org.example.bill.web.dto;

import java.util.List;

public record AdminUserView(
        Long id,
        String username,
        String passwordPlain,
        boolean enabled,
        List<String> roleCodes,
        List<String> boundPhones) {}
