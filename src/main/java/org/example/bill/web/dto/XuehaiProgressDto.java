package org.example.bill.web.dto;

public record XuehaiProgressDto(
        long bookId, Long chapterId, int offset, String updatedAt) {}
