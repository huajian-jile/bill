package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.PhoneBindQueueService;
import org.example.bill.service.UserPhoneService;
import org.example.bill.web.dto.PhoneBindRequest;
import org.example.bill.web.dto.PhoneBindResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/phones")
@RequiredArgsConstructor
public class MePhoneController {

    private final SecurityUtil securityUtil;
    private final UserPhoneService userPhoneService;
    private final PhoneBindQueueService phoneBindQueueService;

    @GetMapping
    public List<String> list() {
        Long uid = requireUid();
        return userPhoneService.listMobiles(uid);
    }

    @PostMapping
    public PhoneBindResponse add(@RequestBody PhoneBindRequest req) {
        Long uid = requireUid();
        boolean immediate = phoneBindQueueService.requestBindOrApproveDirect(uid, req.mobile());
        return new PhoneBindResponse(immediate ? "immediate" : "pending_review");
    }

    private Long requireUid() {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return uid;
    }
}
