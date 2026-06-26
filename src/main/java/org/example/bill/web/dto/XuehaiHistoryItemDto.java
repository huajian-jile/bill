package org.example.bill.web.dto;

public record XuehaiHistoryItemDto(
        long id,
        long bookId,
        String bookTitle,
        String author,
        Long chapterId,
        String chapterTitle,
        double progress,
        String readAt,
        boolean hasCover) {}
