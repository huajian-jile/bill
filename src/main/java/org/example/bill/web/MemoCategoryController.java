package org.example.bill.web;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.MemoCategoryService;
import org.example.bill.web.dto.MemoCategoryDto;
import org.example.bill.web.dto.MemoCategorySaveRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/memo/categories")
@RequiredArgsConstructor
public class MemoCategoryController {

    private final MemoCategoryService memoCategoryService;

    @GetMapping
    public List<MemoCategoryDto> list(@RequestParam String scopeKey) {
        return memoCategoryService.list(scopeKey);
    }

    @PostMapping
    public void save(@RequestBody MemoCategorySaveRequest request) {
        memoCategoryService.save(request);
    }

    @DeleteMapping
    public void clear(@RequestParam String scopeKey) {
        memoCategoryService.clear(scopeKey);
    }
}
