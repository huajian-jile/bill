package org.example.bill.web;

import lombok.RequiredArgsConstructor;
import org.example.bill.domain.WechatBillImport;
import org.example.bill.service.WechatXlsxImportService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class BillImportController {

    private final WechatXlsxImportService wechatImportService;

    @PostMapping(value = "/wechat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public WechatBillImport importWechat(
            @RequestPart("file") MultipartFile file, @RequestParam("mobileCn") String mobileCn)
            throws Exception {
        return wechatImportService.importWechat(file, mobileCn);
    }

    @PostMapping(value = "/alipay", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public WechatBillImport importAlipay(
            @RequestPart("file") MultipartFile file, @RequestParam("mobileCn") String mobileCn)
            throws Exception {
        return wechatImportService.importAlipay(file, mobileCn);
    }
}
