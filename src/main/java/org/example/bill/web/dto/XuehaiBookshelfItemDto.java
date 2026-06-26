package org.example.bill.web.dto;

public record XuehaiBookshelfItemDto(
        XuehaiBookSummaryDto book,
        boolean pinned,
        int pinOrder,
        Long lastReadChapterId,
        String lastReadChapterTitle,
        String lastReadAt,
        String addedAt) {}
