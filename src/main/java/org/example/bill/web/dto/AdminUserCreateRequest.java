package org.example.bill.web.dto;

import java.util.List;

public record AdminUserCreateRequest(String mobile, String password, List<String> roleCodes) {}
