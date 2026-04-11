package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.UserPhoneService;
import org.example.bill.web.dto.PhoneBindRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/phones")
@RequiredArgsConstructor
public class MePhoneController {

    private final SecurityUtil securityUtil;
    private final UserPhoneService userPhoneService;

    @GetMapping
    public List<String> list() {
        Long uid = requireUid();
        return userPhoneService.listMobiles(uid);
    }

    @PostMapping
    public void add(@RequestBody PhoneBindRequest req) {
        Long uid = requireUid();
        userPhoneService.addPhone(uid, req.mobile());
    }

    private Long requireUid() {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return uid;
    }
}
