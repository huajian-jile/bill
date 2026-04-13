package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.AlipayBillImport;
import org.example.bill.repo.AlipayBillImportRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alipay-imports")
@RequiredArgsConstructor
public class AlipayBillImportQueryController {

    private final AlipayBillImportRepository repo;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ANALYTICS')")
    public List<AlipayBillImport> list(@RequestParam(required = false) Long alipayUserId) {
        if (alipayUserId != null) {
            return repo.findByUserId(alipayUserId);
        }
        return repo.findAll();
    }
}
