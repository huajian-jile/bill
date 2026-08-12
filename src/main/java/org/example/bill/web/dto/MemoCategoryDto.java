package org.example.bill.web.dto;

public record MemoCategoryDto(Long id, String name, int sortOrder, Long[] txIds) {}
