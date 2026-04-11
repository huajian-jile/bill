package org.example.bill.web;

import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.BkpRestoreService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bkp/restore")
@RequiredArgsConstructor
public class BkpRestoreController {

    private final BkpRestoreService restoreService;
    private final SecurityUtil securityUtil;

    @PostMapping("/wechat/day")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> wechatDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        int n = restoreService.restoreWechatDay(date, securityUtil.currentUserId());
        return Map.of("restored", n);
    }

    @PostMapping("/wechat/month")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> wechatMonth(@RequestParam int year, @RequestParam int month) {
        int n = restoreService.restoreWechatMonth(year, month, securityUtil.currentUserId());
        return Map.of("restored", n);
    }

    @PostMapping("/wechat/year")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> wechatYear(@RequestParam int year) {
        int n = restoreService.restoreWechatYear(year, securityUtil.currentUserId());
        return Map.of("restored", n);
    }

    @PostMapping("/wechat/all")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> wechatAll() {
        int n = restoreService.restoreWechatAll(securityUtil.currentUserId());
        return Map.of("restored", n);
    }

    @PostMapping("/alipay/day")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> alipayDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        int n = restoreService.restoreAlipayDay(date, securityUtil.currentUserId());
        return Map.of("restored", n);
    }

    @PostMapping("/alipay/month")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> alipayMonth(@RequestParam int year, @RequestParam int month) {
        int n = restoreService.restoreAlipayMonth(year, month, securityUtil.currentUserId());
        return Map.of("restored", n);
    }

    @PostMapping("/alipay/year")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> alipayYear(@RequestParam int year) {
        int n = restoreService.restoreAlipayYear(year, securityUtil.currentUserId());
        return Map.of("restored", n);
    }

    @PostMapping("/alipay/all")
    @PreAuthorize("hasAuthority('PERM_BKP_TX_CRUD')")
    public Map<String, Integer> alipayAll() {
        int n = restoreService.restoreAlipayAll(securityUtil.currentUserId());
        return Map.of("restored", n);
    }
}
