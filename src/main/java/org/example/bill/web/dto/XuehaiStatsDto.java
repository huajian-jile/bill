package org.example.bill.web.dto;

import java.util.List;

public record XuehaiStatsDto(
        long totalBooks,
        long totalChapters,
        long totalLikes,
        long totalFavorites,
        /** 已上传全本文件的书籍数 */
        long booksWithMainFile,
        /** 平均每本书章节数（无书时为 0） */
        double avgChaptersPerBook,
        List<XuehaiBookRankDto> topByLikes,
        List<XuehaiBookRankDto> topByFavorites,
        List<XuehaiMonthBucketDto> booksPerMonth) {}
