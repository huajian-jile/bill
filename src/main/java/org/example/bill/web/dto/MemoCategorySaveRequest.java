package org.example.bill.web.dto;

import java.util.List;

public record MemoCategorySaveRequest(
        String scopeKey,
        List<CategoryItem> categories) {

    public record CategoryItem(
            Long id,
            String name,
            int sortOrder,
            Long[] txIds) {}
}
