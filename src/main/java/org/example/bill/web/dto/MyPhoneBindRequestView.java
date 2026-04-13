package org.example.bill.web.dto;

import java.time.Instant;

/** 当前登录用户查看自己的手机号绑定申请（待审 / 已通过 / 已拒绝）。 */
public record MyPhoneBindRequestView(
        Long id,
        String mobileCn,
        String status,
        Instant createdAt,
        Instant reviewedAt,
        String rejectReason) {}
