package org.example.bill.web;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.WechatBillImport;
import org.example.bill.service.WechatXlsxImportService;
import org.example.bill.web.dto.BatchImportResultDto;
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
    @PreAuthorize("hasAuthority('PERM_IMPORT_XLSX')")
    public WechatBillImport importWechat(
            @RequestPart("file") MultipartFile file, @RequestParam("mobileCn") String mobileCn)
            throws Exception {
        return wechatImportService.importWechat(file, mobileCn);
    }

    /**
     * 多文件单请求。循环在 Controller 调用 {@link WechatXlsxImportService}，保证每文件独立事务，与分次单文件上传一致。
     */
    @PostMapping(value = "/wechat/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_IMPORT_XLSX')")
    public BatchImportResultDto importWechatBatch(
            @RequestParam("files") MultipartFile[] files, @RequestParam("mobileCn") String mobileCn)
            throws Exception {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("请至少选择一个文件");
        }
        List<Long> ids = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            ids.add(wechatImportService.importWechat(f, mobileCn).getId());
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("无有效文件");
        }
        return new BatchImportResultDto(ids.size(), ids);
    }

    @PostMapping(value = "/alipay", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_IMPORT_XLSX')")
    public WechatBillImport importAlipay(
            @RequestPart("file") MultipartFile file, @RequestParam("mobileCn") String mobileCn)
            throws Exception {
        return wechatImportService.importAlipay(file, mobileCn);
    }

    @PostMapping(value = "/alipay/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PERM_IMPORT_XLSX')")
    public BatchImportResultDto importAlipayBatch(
            @RequestParam("files") MultipartFile[] files, @RequestParam("mobileCn") String mobileCn)
            throws Exception {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("请至少选择一个文件");
        }
        List<Long> ids = new ArrayList<>();
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            ids.add(wechatImportService.importAlipay(f, mobileCn).getId());
        }
        if (ids.isEmpty()) {
            throw new IllegalArgumentException("无有效文件");
        }
        return new BatchImportResultDto(ids.size(), ids);
    }
}
