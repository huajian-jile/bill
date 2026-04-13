package org.example.bill.web.dto;

import java.time.Instant;

/** 已通过 / 已拒绝 的绑定申请，供管理员查看审核记录。 */
public record PhoneBindHistoryView(
        Long id,
        Long userId,
        String username,
        String mobileCn,
        Instant createdAt,
        String status,
        Instant reviewedAt,
        String reviewedByUsername,
        String rejectReason) {}
