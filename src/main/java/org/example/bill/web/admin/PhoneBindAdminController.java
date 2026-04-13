package org.example.bill.web.admin;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AppUser;
import org.example.bill.domain.PhoneBindQueue;
import org.example.bill.repo.AppUserRepository;
import org.example.bill.service.PhoneBindQueueService;
import org.example.bill.web.SecurityUtil;
import org.example.bill.web.dto.PhoneBindHistoryView;
import org.example.bill.web.dto.PhoneBindQueueAdminView;
import org.example.bill.web.dto.PhoneBindRejectRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/phone-bind-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_USER_ADMIN')")
public class PhoneBindAdminController {

    private final PhoneBindQueueService phoneBindQueueService;
    private final AppUserRepository appUserRepository;
    private final SecurityUtil securityUtil;

    @GetMapping
    public List<PhoneBindQueueAdminView> listPending() {
        return phoneBindQueueService.listPendingAll().stream()
                .map(this::toView)
                .toList();
    }

    /** 已通过 / 已拒绝记录（数据库中一直保留，仅此前未在管理页展示）。 */
    @GetMapping("/history")
    public List<PhoneBindHistoryView> listHistory() {
        return phoneBindQueueService.listProcessedHistory().stream()
                .map(this::toHistoryView)
                .toList();
    }

    @PostMapping("/{id}/approve")
    public void approve(@PathVariable Long id) {
        Long reviewer = requireReviewer();
        phoneBindQueueService.approve(id, reviewer);
    }

    @PostMapping("/{id}/reject")
    public void reject(
            @PathVariable Long id, @RequestBody(required = false) PhoneBindRejectRequest body) {
        Long reviewer = requireReviewer();
        String reason = body != null ? body.reason() : null;
        phoneBindQueueService.reject(id, reviewer, reason);
    }

    private PhoneBindQueueAdminView toView(PhoneBindQueue row) {
        String username =
                appUserRepository
                        .findById(row.getUserId())
                        .map(AppUser::getUsername)
                        .orElse("?");
        return new PhoneBindQueueAdminView(
                row.getId(), row.getUserId(), username, row.getMobileCn(), row.getCreatedAt());
    }

    private PhoneBindHistoryView toHistoryView(PhoneBindQueue row) {
        String username =
                appUserRepository
                        .findById(row.getUserId())
                        .map(AppUser::getUsername)
                        .orElse("?");
        String reviewer =
                row.getReviewedByUserId() == null
                        ? "—"
                        : appUserRepository
                                .findById(row.getReviewedByUserId())
                                .map(AppUser::getUsername)
                                .orElse("?");
        return new PhoneBindHistoryView(
                row.getId(),
                row.getUserId(),
                username,
                row.getMobileCn(),
                row.getCreatedAt(),
                row.getStatus(),
                row.getReviewedAt(),
                reviewer,
                row.getRejectReason());
    }

    private Long requireReviewer() {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return uid;
    }
}
