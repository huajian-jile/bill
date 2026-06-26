package org.example.bill.web.dto;

public record XuehaiContinueReadDto(
        long bookId,
        String title,
        String author,
        Long chapterId,
        String chapterTitle,
        boolean hasCover,
        String updatedAt) {}
