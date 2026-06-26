package org.example.bill.web.dto;

import java.util.List;

public record XuehaiBookSummaryDto(
        long id,
        String title,
        String author,
        String summary,
        int likeCount,
        int favoriteCount,
        int chapterCount,
        boolean hasMainFile,
        String mainFileName,
        boolean hasCover,
        boolean likedByMe,
        boolean favoritedByMe,
        String contentType,
        String status,
        List<String> tags,
        int wordCount,
        double avgScore,
        int ratingCount,
        boolean onBookshelf,
        Integer myRating) {}
