package org.example.bill.web.dto;

import java.util.List;

public record XuehaiBookDetailDto(
        long id,
        String title,
        String author,
        String summary,
        int likeCount,
        int favoriteCount,
        boolean hasMainFile,
        boolean hasCover,
        boolean likedByMe,
        boolean favoritedByMe,
        boolean onBookshelf,
        String mainFileName,
        String contentType,
        String status,
        List<String> tags,
        int wordCount,
        double avgScore,
        int ratingCount,
        Integer myRating,
        String publishAt,
        List<XuehaiChapterViewDto> chapters,
        List<XuehaiBookSummaryDto> similarBooks) {}
