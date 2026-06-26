package org.example.bill.web.admin;

import lombok.RequiredArgsConstructor;
import org.example.bill.service.AdminImportCleanupService;
import org.example.bill.service.AdminImportCleanupService.DeleteResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/import-cleanup")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PERM_VIEW_ALL_BILLS')")
public class AdminImportCleanupController {

    private final AdminImportCleanupService service;

    /** 按手机号 + 渠道删除导入记录与明细（渠道可选：WECHAT / ALIPAY / ALL）。 */
    @DeleteMapping
    public DeleteResult deleteByMobile(
            @RequestParam String mobileCn,
            @RequestParam(required = false, defaultValue = "ALL") String channel) {
        return service.deleteImportsByMobileAndChannel(mobileCn, channel);
    }
}

