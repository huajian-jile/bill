package org.example.bill.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.util.List;

public record AdminUserCreateRequest(
        @JsonAlias("mobile") String username, String password, List<String> roleCodes) {}
