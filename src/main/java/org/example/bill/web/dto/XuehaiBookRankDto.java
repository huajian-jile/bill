package org.example.bill.web.dto;

public record XuehaiBookRankDto(
        long id, String title, String author, int likeCount, int favoriteCount, long chapterCount) {}
