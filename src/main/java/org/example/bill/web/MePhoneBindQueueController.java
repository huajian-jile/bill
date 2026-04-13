package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.PhoneBindQueue;
import org.example.bill.service.PhoneBindQueueService;
import org.example.bill.web.dto.MyPhoneBindRequestView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/phone-bind-requests")
@RequiredArgsConstructor
public class MePhoneBindQueueController {

    private final SecurityUtil securityUtil;
    private final PhoneBindQueueService phoneBindQueueService;

    @GetMapping
    public List<MyPhoneBindRequestView> listMine() {
        Long uid = requireUid();
        return phoneBindQueueService.listAllForUser(uid).stream()
                .map(this::toView)
                .toList();
    }

    private MyPhoneBindRequestView toView(PhoneBindQueue row) {
        return new MyPhoneBindRequestView(
                row.getId(),
                row.getMobileCn(),
                row.getStatus(),
                row.getCreatedAt(),
                row.getReviewedAt(),
                row.getRejectReason());
    }

    private Long requireUid() {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return uid;
    }
}
