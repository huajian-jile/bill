package org.example.bill.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.example.bill.config.XuehaiProperties;
import org.example.bill.domain.XuehaiBook;
import org.example.bill.domain.XuehaiBookFavorite;
import org.example.bill.domain.XuehaiBookLike;
import org.example.bill.domain.XuehaiBookRating;
import org.example.bill.domain.XuehaiBookshelf;
import org.example.bill.domain.XuehaiChapter;
import org.example.bill.domain.XuehaiReadHistory;
import org.example.bill.domain.XuehaiReadProgress;
import org.example.bill.domain.XuehaiUserBookKey;
import org.example.bill.repo.XuehaiBookFavoriteRepository;
import org.example.bill.repo.XuehaiBookLikeRepository;
import org.example.bill.repo.XuehaiBookRatingRepository;
import org.example.bill.repo.XuehaiBookRepository;
import org.example.bill.repo.XuehaiBookshelfRepository;
import org.example.bill.repo.XuehaiChapterRepository;
import org.example.bill.repo.XuehaiReadHistoryRepository;
import org.example.bill.repo.XuehaiReadProgressRepository;
import org.example.bill.web.dto.XuehaiBookDetailDto;
import org.example.bill.web.dto.XuehaiBookRankDto;
import org.example.bill.web.dto.XuehaiBookSummaryDto;
import org.example.bill.web.dto.XuehaiBookshelfItemDto;
import org.example.bill.web.dto.XuehaiChapterViewDto;
import org.example.bill.web.dto.XuehaiContinueReadDto;
import org.example.bill.web.dto.XuehaiHistoryItemDto;
import org.example.bill.web.dto.XuehaiMonthBucketDto;
import org.example.bill.web.dto.XuehaiPageDto;
import org.example.bill.web.dto.XuehaiProgressDto;
import org.example.bill.web.dto.XuehaiRankItemDto;
import org.example.bill.web.dto.XuehaiStatsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class XuehaiService {

    private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

    private final XuehaiProperties props;
    private final XuehaiBookRepository bookRepository;
    private final XuehaiChapterRepository chapterRepository;
    private final XuehaiBookLikeRepository likeRepository;
    private final XuehaiBookFavoriteRepository favoriteRepository;
    private final XuehaiBookshelfRepository bookshelfRepository;
    private final XuehaiBookRatingRepository ratingRepository;
    private final XuehaiReadHistoryRepository historyRepository;
    private final XuehaiReadProgressRepository progressRepository;

    private static final int HISTORY_LIMIT = 200;
    private static final int MAX_PINNED = 9;

    public List<XuehaiBookSummaryDto> listBooks(Long userId) {
        return listBooksPaged(userId, null, null, null, "latest", 0, 500).items();
    }

    public XuehaiPageDto<XuehaiBookSummaryDto> listBooksPaged(
            Long userId,
            String contentType,
            String status,
            String keyword,
            String sort,
            int page,
            int size) {
        requireUser(userId);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, resolveBookSort(sort));
        Page<XuehaiBook> result =
                bookRepository.search(
                        blankToNull(contentType), blankToNull(status), keyword, pageable);
        List<XuehaiBookSummaryDto> items =
                result.getContent().stream().map(b -> toSummary(b, userId)).toList();
        return new XuehaiPageDto<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public XuehaiPageDto<XuehaiRankItemDto> listRank(Long userId, String type, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize);
        Page<XuehaiBook> result =
                switch (type == null ? "" : type) {
                    case "recommend" -> bookRepository.rankRecommend(pageable);
                    case "finished" -> bookRepository.rankFinished(pageable);
                    case "new_book" ->
                            bookRepository.rankNewBook(
                                    Instant.now().minus(java.time.Duration.ofDays(30)), pageable);
                    default ->
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未知榜单类型");
                };
        List<XuehaiRankItemDto> items = new ArrayList<>();
        int base = result.getNumber() * result.getSize();
        int i = 0;
        for (XuehaiBook b : result.getContent()) {
            items.add(new XuehaiRankItemDto(toSummary(b, userId), base + i + 1));
            i++;
        }
        return new XuehaiPageDto<>(items, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public XuehaiBookDetailDto getBookDetail(Long bookId, Long userId) {
        XuehaiBook b = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        List<XuehaiChapter> ch =
                chapterRepository.findByBookIdOrderBySortOrderAscIdAsc(bookId);
        List<XuehaiChapterViewDto> chViews = new ArrayList<>();
        for (XuehaiChapter c : ch) {
            chViews.add(
                    new XuehaiChapterViewDto(
                            c.getId(),
                            c.getTitle(),
                            c.getSortOrder(),
                            c.getFileOriginalName(),
                            ISO_INSTANT.format(c.getCreatedAt())));
        }
        boolean liked = userId != null && likeRepository.existsByIdUserIdAndIdBookId(userId, bookId);
        boolean fav =
                userId != null && favoriteRepository.existsByIdUserIdAndIdBookId(userId, bookId);
        boolean onShelf =
                userId != null && bookshelfRepository.existsByIdUserIdAndIdBookId(userId, bookId);
        Integer myRating = userId != null ? myRatingScore(userId, bookId) : null;
        List<XuehaiBook> similar =
                b.getContentType() != null
                        ? bookRepository.findTop6ByContentTypeAndIdNotOrderByReadCountDesc(
                                b.getContentType(), bookId)
                        : bookRepository.findTop6ByIdNotOrderByReadCountDesc(bookId);
        List<XuehaiBookSummaryDto> similarDtos =
                similar.stream().map(s -> toSummary(s, userId)).toList();
        String publishAt =
                b.getPublishAt() != null ? ISO_INSTANT.format(b.getPublishAt()) : null;
        return new XuehaiBookDetailDto(
                b.getId(),
                b.getTitle(),
                b.getAuthor(),
                b.getSummary(),
                b.getLikeCount(),
                b.getFavoriteCount(),
                b.getMainFileStoragePath() != null && !b.getMainFileStoragePath().isBlank(),
                b.getCoverStoragePath() != null && !b.getCoverStoragePath().isBlank(),
                liked,
                fav,
                onShelf,
                b.getMainFileOriginalName(),
                b.getContentType(),
                b.getStatus(),
                parseTags(b.getTags()),
                b.getWordCount(),
                roundScore(b.getAvgScore()),
                b.getRatingCount(),
                myRating,
                publishAt,
                chViews,
                similarDtos);
    }

    @Transactional
    public XuehaiBookDetailDto createBook(
            Long userId,
            String title,
            String author,
            String summary,
            MultipartFile mainFile,
            MultipartFile coverFile)
            throws IOException {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "书名不能为空");
        }
        XuehaiBook b = new XuehaiBook();
        b.setTitle(title.trim());
        b.setAuthor(trimToNull(author));
        b.setSummary(trimToNull(summary));
        b.setCreatedByUserId(userId);
        b.setLikeCount(0);
        b.setFavoriteCount(0);
        b.setContentType("other");
        b.setStatus("ongoing");
        b.setWordCount(0);
        b.setAvgScore(0);
        b.setRatingCount(0);
        b.setReadCount(0);
        b.setRecommendWeight(0);
        b.setPublishAt(Instant.now());
        b = bookRepository.save(b);
        bookRepository.flush();

        if (coverFile != null && !coverFile.isEmpty()) {
            Stored s = storeFile(b.getId(), coverFile, "cover");
            b.setCoverStoragePath(s.relativePath());
        }
        if (mainFile != null && !mainFile.isEmpty()) {
            Stored s = storeFile(b.getId(), mainFile, "main");
            b.setMainFileStoragePath(s.relativePath());
            b.setMainFileOriginalName(s.originalName());
        }
        bookRepository.save(b);
        return getBookDetail(b.getId(), userId);
    }

    @Transactional
    public XuehaiChapterViewDto addChapter(
            Long userId, Long bookId, String title, Integer sortOrder, MultipartFile file)
            throws IOException {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        XuehaiBook book = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "章节标题不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请上传章节文件");
        }
        XuehaiChapter ch = new XuehaiChapter();
        ch.setBookId(book.getId());
        ch.setTitle(title.trim());
        ch.setSortOrder(sortOrder == null ? 0 : sortOrder);
        Stored s = storeFile(book.getId(), file, "ch");
        ch.setFileStoragePath(s.relativePath());
        ch.setFileOriginalName(s.originalName());
        ch = chapterRepository.save(ch);
        return new XuehaiChapterViewDto(
                ch.getId(),
                ch.getTitle(),
                ch.getSortOrder(),
                ch.getFileOriginalName(),
                ISO_INSTANT.format(ch.getCreatedAt()));
    }

    @Transactional
    public boolean toggleLike(Long userId, Long bookId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        XuehaiBook book = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        XuehaiUserBookKey key = new XuehaiUserBookKey(userId, bookId);
        if (likeRepository.existsByIdUserIdAndIdBookId(userId, bookId)) {
            likeRepository.deleteByIdUserIdAndIdBookId(userId, bookId);
            book.setLikeCount(Math.max(0, book.getLikeCount() - 1));
            bookRepository.save(book);
            return false;
        }
        XuehaiBookLike row = new XuehaiBookLike();
        row.setId(key);
        row.setCreatedAt(Instant.now());
        likeRepository.save(row);
        book.setLikeCount(book.getLikeCount() + 1);
        bookRepository.save(book);
        return true;
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long bookId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        XuehaiBook book = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        XuehaiUserBookKey key = new XuehaiUserBookKey(userId, bookId);
        if (favoriteRepository.existsByIdUserIdAndIdBookId(userId, bookId)) {
            favoriteRepository.deleteByIdUserIdAndIdBookId(userId, bookId);
            book.setFavoriteCount(Math.max(0, book.getFavoriteCount() - 1));
            bookRepository.save(book);
            return false;
        }
        XuehaiBookFavorite row = new XuehaiBookFavorite();
        row.setId(key);
        row.setCreatedAt(Instant.now());
        favoriteRepository.save(row);
        book.setFavoriteCount(book.getFavoriteCount() + 1);
        bookRepository.save(book);
        return true;
    }

    @Transactional
    public void rateBook(Long userId, long bookId, int score) {
        requireUser(userId);
        if (score < 1 || score > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评分为 1～5 星");
        }
        XuehaiBook book = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        XuehaiUserBookKey key = new XuehaiUserBookKey(userId, bookId);
        XuehaiBookRating row =
                ratingRepository.findByIdUserIdAndIdBookId(userId, bookId).orElse(null);
        if (row == null) {
            row = new XuehaiBookRating();
            row.setId(key);
            row.setScore(score);
            row.setUpdatedAt(Instant.now());
            ratingRepository.save(row);
            book.setRatingCount(book.getRatingCount() + 1);
        } else {
            row.setScore(score);
            row.setUpdatedAt(Instant.now());
            ratingRepository.save(row);
        }
        refreshBookAvgScore(book);
        bookRepository.save(book);
    }

    @Transactional
    public void addToBookshelf(Long userId, long bookId) {
        requireUser(userId);
        bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        if (bookshelfRepository.existsByIdUserIdAndIdBookId(userId, bookId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已在书架");
        }
        XuehaiBookshelf row = new XuehaiBookshelf();
        row.setId(new XuehaiUserBookKey(userId, bookId));
        row.setAddedAt(Instant.now());
        row.setPinned(false);
        row.setPinOrder(0);
        bookshelfRepository.save(row);
    }

    @Transactional
    public void removeFromBookshelf(Long userId, long bookId) {
        requireUser(userId);
        bookshelfRepository.deleteByIdUserIdAndIdBookId(userId, bookId);
    }

    @Transactional
    public void pinBookshelf(Long userId, long bookId, boolean pinned) {
        requireUser(userId);
        XuehaiBookshelf row =
                bookshelfRepository
                        .findByIdUserIdAndIdBookId(userId, bookId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "不在书架"));
        if (pinned) {
            long pinnedCount = bookshelfRepository.countByIdUserIdAndPinnedTrue(userId);
            if (!row.isPinned() && pinnedCount >= MAX_PINNED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "最多置顶 9 本");
            }
            row.setPinned(true);
            row.setPinOrder((int) pinnedCount + 1);
        } else {
            row.setPinned(false);
            row.setPinOrder(0);
        }
        bookshelfRepository.save(row);
    }

    public List<XuehaiBookshelfItemDto> listBookshelf(Long userId, String sort) {
        requireUser(userId);
        List<XuehaiBookshelf> rows =
                bookshelfRepository.findByIdUserIdOrderByPinnedDescPinOrderAscLastReadAtDescAddedAtDesc(
                        userId);
        if ("title".equals(sort)) {
            rows = new ArrayList<>(rows);
            rows.sort(
                    (a, b) -> {
                        XuehaiBook ba = bookRepository.findById(a.getId().getBookId()).orElseThrow();
                        XuehaiBook bb = bookRepository.findById(b.getId().getBookId()).orElseThrow();
                        return ba.getTitle().compareToIgnoreCase(bb.getTitle());
                    });
        } else if ("added".equals(sort)) {
            rows = new ArrayList<>(rows);
            rows.sort((a, b) -> b.getAddedAt().compareTo(a.getAddedAt()));
        }
        List<XuehaiBookshelfItemDto> out = new ArrayList<>();
        for (XuehaiBookshelf s : rows) {
            XuehaiBook b = bookRepository.findById(s.getId().getBookId()).orElseThrow();
            String chTitle = null;
            if (s.getLastReadChapterId() != null) {
                chTitle =
                        chapterRepository
                                .findById(s.getLastReadChapterId())
                                .map(XuehaiChapter::getTitle)
                                .orElse(null);
            }
            out.add(
                    new XuehaiBookshelfItemDto(
                            toSummary(b, userId),
                            s.isPinned(),
                            s.getPinOrder(),
                            s.getLastReadChapterId(),
                            chTitle,
                            s.getLastReadAt() != null ? ISO_INSTANT.format(s.getLastReadAt()) : null,
                            ISO_INSTANT.format(s.getAddedAt())));
        }
        return out;
    }

    public List<XuehaiBookSummaryDto> listFavorites(Long userId) {
        requireUser(userId);
        return favoriteRepository.findByIdUserIdOrderByCreatedAtDesc(userId).stream()
                .map(f -> bookRepository.findById(f.getId().getBookId()).orElse(null))
                .filter(b -> b != null)
                .map(b -> toSummary(b, userId))
                .toList();
    }

    @Transactional
    public void addFavorite(Long userId, long bookId) {
        requireUser(userId);
        if (!favoriteRepository.existsByIdUserIdAndIdBookId(userId, bookId)) {
            toggleFavorite(userId, bookId);
        }
    }

    @Transactional
    public void removeFavorite(Long userId, long bookId) {
        requireUser(userId);
        if (favoriteRepository.existsByIdUserIdAndIdBookId(userId, bookId)) {
            toggleFavorite(userId, bookId);
        }
    }

    @Transactional
    public XuehaiProgressDto saveProgress(
            Long userId, long bookId, Long chapterId, int offset) {
        requireUser(userId);
        XuehaiBook book = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        if (chapterId != null) {
            XuehaiChapter ch = chapterRepository.findById(chapterId).orElseThrow(() -> chapterNotFound());
            if (!ch.getBookId().equals(bookId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "章节不属于该书");
            }
        }
        XuehaiUserBookKey key = new XuehaiUserBookKey(userId, bookId);
        XuehaiReadProgress prog =
                progressRepository.findByIdUserIdAndIdBookId(userId, bookId).orElse(null);
        if (prog == null) {
            prog = new XuehaiReadProgress();
            prog.setId(key);
        }
        prog.setChapterId(chapterId);
        prog.setOffsetPos(Math.max(0, offset));
        prog.setUpdatedAt(Instant.now());
        progressRepository.save(prog);

        XuehaiReadHistory hist = new XuehaiReadHistory();
        hist.setUserId(userId);
        hist.setBookId(bookId);
        hist.setChapterId(chapterId);
        hist.setProgress(offset);
        hist.setReadAt(Instant.now());
        historyRepository.save(hist);
        trimHistory(userId);

        bookshelfRepository
                .findByIdUserIdAndIdBookId(userId, bookId)
                .ifPresent(
                        s -> {
                            s.setLastReadChapterId(chapterId);
                            s.setLastReadAt(Instant.now());
                            bookshelfRepository.save(s);
                        });

        book.setReadCount(book.getReadCount() + 1);
        bookRepository.save(book);

        return new XuehaiProgressDto(
                bookId, chapterId, prog.getOffsetPos(), ISO_INSTANT.format(prog.getUpdatedAt()));
    }

    public XuehaiProgressDto getProgress(Long userId, long bookId) {
        requireUser(userId);
        return progressRepository
                .findByIdUserIdAndIdBookId(userId, bookId)
                .map(
                        p ->
                                new XuehaiProgressDto(
                                        bookId,
                                        p.getChapterId(),
                                        p.getOffsetPos(),
                                        ISO_INSTANT.format(p.getUpdatedAt())))
                .orElse(new XuehaiProgressDto(bookId, null, 0, null));
    }

    public XuehaiContinueReadDto continueReading(Long userId) {
        requireUser(userId);
        Optional<XuehaiReadProgress> latest =
                progressRepository.findByIdUserIdOrderByUpdatedAtDesc(userId).stream().findFirst();
        if (latest.isEmpty()) {
            return null;
        }
        XuehaiReadProgress p = latest.get();
        XuehaiBook b =
                bookRepository.findById(p.getId().getBookId()).orElseThrow(() -> bookNotFound());
        String chTitle = null;
        if (p.getChapterId() != null) {
            chTitle =
                    chapterRepository.findById(p.getChapterId()).map(XuehaiChapter::getTitle).orElse(null);
        }
        return new XuehaiContinueReadDto(
                b.getId(),
                b.getTitle(),
                b.getAuthor(),
                p.getChapterId(),
                chTitle,
                b.getCoverStoragePath() != null && !b.getCoverStoragePath().isBlank(),
                ISO_INSTANT.format(p.getUpdatedAt()));
    }

    public List<XuehaiHistoryItemDto> listHistory(Long userId) {
        requireUser(userId);
        List<XuehaiReadHistory> rows =
                historyRepository.findByUserIdOrderByReadAtDesc(
                        userId, PageRequest.of(0, HISTORY_LIMIT));
        List<XuehaiHistoryItemDto> out = new ArrayList<>();
        for (XuehaiReadHistory h : rows) {
            XuehaiBook b = bookRepository.findById(h.getBookId()).orElse(null);
            if (b == null) {
                continue;
            }
            String chTitle = null;
            if (h.getChapterId() != null) {
                chTitle =
                        chapterRepository
                                .findById(h.getChapterId())
                                .map(XuehaiChapter::getTitle)
                                .orElse(null);
            }
            out.add(
                    new XuehaiHistoryItemDto(
                            h.getId(),
                            h.getBookId(),
                            b.getTitle(),
                            b.getAuthor(),
                            h.getChapterId(),
                            chTitle,
                            h.getProgress(),
                            ISO_INSTANT.format(h.getReadAt()),
                            b.getCoverStoragePath() != null && !b.getCoverStoragePath().isBlank()));
        }
        return out;
    }

    @Transactional
    public void deleteHistory(Long userId, long historyId) {
        requireUser(userId);
        historyRepository.deleteByIdAndUserId(historyId, userId);
    }

    @Transactional
    public void clearHistory(Long userId) {
        requireUser(userId);
        historyRepository.deleteAllByUserId(userId);
    }

    public void requireAuthForContent(Long userId) {
        requireUser(userId);
    }

    public XuehaiStatsDto stats() {
        long totalBooks = bookRepository.count();
        long totalChapters = chapterRepository.count();
        long totalLikes = likeRepository.count();
        long totalFavorites = favoriteRepository.count();

        List<XuehaiBook> top = bookRepository.findTop10ByOrderByLikeCountDescIdDesc();
        List<XuehaiBookRankDto> ranks = new ArrayList<>();
        for (XuehaiBook b : top) {
            ranks.add(
                    new XuehaiBookRankDto(
                            b.getId(),
                            b.getTitle(),
                            b.getAuthor(),
                            b.getLikeCount(),
                            b.getFavoriteCount(),
                            chapterRepository.countByBookId(b.getId())));
        }

        List<XuehaiBook> topFav = bookRepository.findTop10ByOrderByFavoriteCountDescIdDesc();
        List<XuehaiBookRankDto> rankFav = new ArrayList<>();
        for (XuehaiBook b : topFav) {
            rankFav.add(
                    new XuehaiBookRankDto(
                            b.getId(),
                            b.getTitle(),
                            b.getAuthor(),
                            b.getLikeCount(),
                            b.getFavoriteCount(),
                            chapterRepository.countByBookId(b.getId())));
        }

        long booksWithMain = bookRepository.countByMainFileStoragePathIsNotNull();
        double avgCh =
                totalBooks <= 0 ? 0.0 : ((double) totalChapters / (double) totalBooks);

        List<XuehaiMonthBucketDto> months = new ArrayList<>();
        for (Object[] row : bookRepository.countGroupedByMonth()) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Instant bucketInstant = nativeSqlTimeToInstant(row[0]);
            long cnt = ((Number) row[1]).longValue();
            YearMonth ym = YearMonth.from(bucketInstant.atZone(ZoneOffset.UTC));
            months.add(new XuehaiMonthBucketDto(ym.toString(), cnt));
        }

        return new XuehaiStatsDto(
                totalBooks,
                totalChapters,
                totalLikes,
                totalFavorites,
                booksWithMain,
                avgCh,
                ranks,
                rankFav,
                months);
    }

    /**
     * Hibernate 6 原生查询里日期列可能映射为 {@link Instant}、{@link java.sql.Timestamp} 等，需统一转换。
     */
    private static Instant nativeSqlTimeToInstant(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("null time bucket");
        }
        if (value instanceof Instant i) {
            return i;
        }
        if (value instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        if (value instanceof java.util.Date d) {
            return d.toInstant();
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (value instanceof ZonedDateTime zdt) {
            return zdt.toInstant();
        }
        if (value instanceof java.time.LocalDate ld) {
            return ld.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
        if (value instanceof java.time.LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant();
        }
        throw new IllegalStateException("无法解析 SQL 时间类型: " + value.getClass().getName());
    }

    public Path resolveMainFile(Long bookId) {
        XuehaiBook b = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        if (b.getMainFileStoragePath() == null || b.getMainFileStoragePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "本书未上传全本文件");
        }
        return resolveUnderBase(props.resolvedStorageDir(), b.getMainFileStoragePath());
    }

    public Path resolveCover(Long bookId) {
        XuehaiBook b = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        if (b.getCoverStoragePath() == null || b.getCoverStoragePath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "本书无封面");
        }
        return resolveUnderBase(props.resolvedStorageDir(), b.getCoverStoragePath());
    }

    public Path resolveChapterFile(Long chapterId) {
        XuehaiChapter ch =
                chapterRepository.findById(chapterId).orElseThrow(() -> chapterNotFound());
        return resolveUnderBase(props.resolvedStorageDir(), ch.getFileStoragePath());
    }

    public String chapterOriginalName(Long chapterId) {
        XuehaiChapter ch =
                chapterRepository.findById(chapterId).orElseThrow(() -> chapterNotFound());
        return ch.getFileOriginalName() != null ? ch.getFileOriginalName() : "chapter";
    }

    public String bookMainOriginalName(Long bookId) {
        XuehaiBook b = bookRepository.findById(bookId).orElseThrow(() -> bookNotFound());
        return b.getMainFileOriginalName() != null ? b.getMainFileOriginalName() : "book";
    }

    private XuehaiBookSummaryDto toSummary(XuehaiBook b, Long userId) {
        int ch = (int) chapterRepository.countByBookId(b.getId());
        boolean liked = userId != null && likeRepository.existsByIdUserIdAndIdBookId(userId, b.getId());
        boolean fav =
                userId != null && favoriteRepository.existsByIdUserIdAndIdBookId(userId, b.getId());
        boolean onShelf =
                userId != null && bookshelfRepository.existsByIdUserIdAndIdBookId(userId, b.getId());
        Integer myRating = userId != null ? myRatingScore(userId, b.getId()) : null;
        return new XuehaiBookSummaryDto(
                b.getId(),
                b.getTitle(),
                b.getAuthor(),
                b.getSummary(),
                b.getLikeCount(),
                b.getFavoriteCount(),
                ch,
                b.getMainFileStoragePath() != null && !b.getMainFileStoragePath().isBlank(),
                b.getMainFileOriginalName(),
                b.getCoverStoragePath() != null && !b.getCoverStoragePath().isBlank(),
                liked,
                fav,
                b.getContentType() != null ? b.getContentType() : "other",
                b.getStatus() != null ? b.getStatus() : "ongoing",
                parseTags(b.getTags()),
                b.getWordCount(),
                roundScore(b.getAvgScore()),
                b.getRatingCount(),
                onShelf,
                myRating);
    }

    private static void requireUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }

    private Integer myRatingScore(Long userId, long bookId) {
        return ratingRepository
                .findByIdUserIdAndIdBookId(userId, bookId)
                .map(XuehaiBookRating::getScore)
                .orElse(null);
    }

    private void refreshBookAvgScore(XuehaiBook book) {
        List<Object[]> agg = ratingRepository.aggregateByBookId(book.getId());
        if (agg.isEmpty() || agg.get(0) == null) {
            book.setAvgScore(0);
            return;
        }
        Object[] row = agg.get(0);
        double avg = row[0] != null ? ((Number) row[0]).doubleValue() : 0;
        book.setAvgScore(avg);
    }

    @Transactional
    protected void trimHistory(Long userId) {
        long cnt = historyRepository.countByUserId(userId);
        if (cnt <= HISTORY_LIMIT) {
            return;
        }
        List<Long> stale = historyRepository.findIdsBeyondLimit(userId);
        if (!stale.isEmpty()) {
            historyRepository.deleteAllById(stale);
        }
    }

    private static Sort resolveBookSort(String sort) {
        if ("hot".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "readCount", "id");
        }
        if ("rating".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "avgScore", "id");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt", "id");
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static List<String> parseTags(String tags) {
        if (tags == null || tags.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .limit(8)
                .collect(Collectors.toList());
    }

    private static double roundScore(double s) {
        return Math.round(s * 10.0) / 10.0;
    }

    private Stored storeFile(Long bookId, MultipartFile file, String prefix) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension = extension(original);
        Path base = props.resolvedStorageDir();
        String dir = "book-" + bookId;
        String name = prefix + "-" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path rel = Path.of(dir, name);
        Path full = base.resolve(rel).normalize();
        if (!full.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法存储路径");
        }
        Files.createDirectories(full.getParent());
        file.transferTo(full);
        return new Stored(rel.toString().replace("\\", "/"), original, extension);
    }

    private static Path resolveUnderBase(Path base, String relative) {
        if (relative == null || relative.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Path full = base.resolve(relative).normalize();
        if (!full.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法路径");
        }
        if (!Files.exists(full)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在");
        }
        return full;
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i + 1);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static ResponseStatusException bookNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "书籍不存在");
    }

    private static ResponseStatusException chapterNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "章节不存在");
    }

    private record Stored(String relativePath, String originalName, String extension) {}
}
