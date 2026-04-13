package org.example.bill.web.dto;

import java.time.Instant;

public record PhoneBindQueueAdminView(
        Long id, Long userId, String username, String mobileCn, Instant createdAt) {}
