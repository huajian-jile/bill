package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.WechatUser;
import org.example.bill.repo.WechatUserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wechat-users")
@RequiredArgsConstructor
public class WechatUserQueryController {

    private final WechatUserRepository repo;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public List<WechatUser> list() {
        return repo.findAll();
    }
}
