package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.PersonPhoneLinkService;
import org.example.bill.web.dto.PersonPhoneLinkDto;
import org.example.bill.web.dto.PersonPhoneLinkRequest;
import org.example.bill.web.dto.PhoneOptionDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me/person-phones")
@RequiredArgsConstructor
public class MePersonPhoneController {

    private final SecurityUtil securityUtil;
    private final PersonPhoneLinkService personPhoneLinkService;

    @GetMapping("/linkable-phones")
    public List<PhoneOptionDto> linkablePhones() {
        String mobile = requireMobile();
        return personPhoneLinkService.listLinkablePhones(mobile);
    }

    @GetMapping("/links")
    public List<PersonPhoneLinkDto> links() {
        String mobile = requireMobile();
        return personPhoneLinkService.listLinks(mobile);
    }

    @PostMapping
    public void add(@RequestBody PersonPhoneLinkRequest req) {
        String mobile = requireMobile();
        personPhoneLinkService.addLink(mobile, req.personId(), req.phoneId());
    }

    @DeleteMapping("/{linkId}")
    public void remove(@PathVariable long linkId) {
        String mobile = requireMobile();
        personPhoneLinkService.removeLink(mobile, linkId);
    }

    private String requireMobile() {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return auth.getName().trim();
    }
}
