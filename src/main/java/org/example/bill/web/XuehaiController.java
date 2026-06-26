package org.example.bill.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.bill.service.XuehaiService;
import org.example.bill.web.dto.XuehaiBookDetailDto;
import org.example.bill.web.dto.XuehaiBookSummaryDto;
import org.example.bill.web.dto.XuehaiBookshelfItemDto;
import org.example.bill.web.dto.XuehaiBookshelfRequest;
import org.example.bill.web.dto.XuehaiChapterViewDto;
import org.example.bill.web.dto.XuehaiContinueReadDto;
import org.example.bill.web.dto.XuehaiHistoryItemDto;
import org.example.bill.web.dto.XuehaiPageDto;
import org.example.bill.web.dto.XuehaiProgressDto;
import org.example.bill.web.dto.XuehaiProgressRequest;
import org.example.bill.web.dto.XuehaiRankItemDto;
import org.example.bill.web.dto.XuehaiRatingRequest;
import org.example.bill.web.dto.XuehaiStatsDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/xuehai")
@RequiredArgsConstructor
public class XuehaiController {

    private final XuehaiService xuehaiService;
    private final SecurityUtil securityUtil;

    @GetMapping("/books")
    public Object listBooks(
            @RequestParam(value = "contentType", required = false) String contentType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "q", required = false) String keyword,
            @RequestParam(value = "sort", defaultValue = "latest") String sort,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        Long userId = securityUtil.currentUserId();
        if (page > 0 || size != 20 || contentType != null || status != null || keyword != null) {
            return xuehaiService.listBooksPaged(
                    userId, contentType, status, keyword, sort, page, size);
        }
        return xuehaiService.listBooks(userId);
    }

    @GetMapping("/ranks/{type}")
    public XuehaiPageDto<XuehaiRankItemDto> ranks(
            @PathVariable String type,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return xuehaiService.listRank(securityUtil.currentUserId(), type, page, size);
    }

    @GetMapping("/books/{id}")
    public XuehaiBookDetailDto bookDetail(@PathVariable("id") long id) {
        return xuehaiService.getBookDetail(id, securityUtil.currentUserId());
    }

    @GetMapping("/continue")
    public XuehaiContinueReadDto continueRead() {
        return xuehaiService.continueReading(securityUtil.currentUserId());
    }

    @PostMapping(value = "/books", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public XuehaiBookDetailDto createBook(
            @RequestParam("title") String title,
            @RequestParam(value = "author", required = false) String author,
            @RequestParam(value = "summary", required = false) String summary,
            @RequestPart(value = "mainFile", required = false) MultipartFile mainFile,
            @RequestPart(value = "coverFile", required = false) MultipartFile coverFile)
            throws IOException {
        return xuehaiService.createBook(
                securityUtil.currentUserId(), title, author, summary, mainFile, coverFile);
    }

    @PostMapping(value = "/books/{id}/chapters", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public XuehaiChapterViewDto addChapter(
            @PathVariable("id") long bookId,
            @RequestParam("title") String title,
            @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
            @RequestPart("file") MultipartFile file)
            throws IOException {
        return xuehaiService.addChapter(
                securityUtil.currentUserId(), bookId, title, sortOrder, file);
    }

    @PostMapping("/books/{id}/rating")
    public Map<String, String> rate(
            @PathVariable("id") long bookId, @RequestBody XuehaiRatingRequest req) {
        xuehaiService.rateBook(securityUtil.currentUserId(), bookId, req.score());
        return Map.of("ok", "true");
    }

    @PostMapping("/bookshelf")
    public Map<String, String> addBookshelf(@RequestBody XuehaiBookshelfRequest req) {
        xuehaiService.addToBookshelf(securityUtil.currentUserId(), req.bookId());
        return Map.of("ok", "true");
    }

    @DeleteMapping("/bookshelf/{bookId}")
    public Map<String, String> removeBookshelf(@PathVariable long bookId) {
        xuehaiService.removeFromBookshelf(securityUtil.currentUserId(), bookId);
        return Map.of("ok", "true");
    }

    @PutMapping("/bookshelf/{bookId}/pin")
    public Map<String, String> pinBookshelf(
            @PathVariable long bookId, @RequestParam boolean pinned) {
        xuehaiService.pinBookshelf(securityUtil.currentUserId(), bookId, pinned);
        return Map.of("ok", "true");
    }

    @GetMapping("/bookshelf")
    public List<XuehaiBookshelfItemDto> bookshelf(
            @RequestParam(value = "sort", defaultValue = "recent") String sort) {
        return xuehaiService.listBookshelf(securityUtil.currentUserId(), sort);
    }

    @GetMapping("/favorites")
    public List<XuehaiBookSummaryDto> favorites() {
        return xuehaiService.listFavorites(securityUtil.currentUserId());
    }

    @PostMapping("/favorites")
    public Map<String, String> addFavorite(@RequestBody XuehaiBookshelfRequest req) {
        xuehaiService.addFavorite(securityUtil.currentUserId(), req.bookId());
        return Map.of("ok", "true");
    }

    @DeleteMapping("/favorites/{bookId}")
    public Map<String, String> removeFavorite(@PathVariable long bookId) {
        xuehaiService.removeFavorite(securityUtil.currentUserId(), bookId);
        return Map.of("ok", "true");
    }

    @PostMapping("/books/{id}/like/toggle")
    public Map<String, Object> toggleLike(@PathVariable("id") long bookId) {
        boolean on = xuehaiService.toggleLike(securityUtil.currentUserId(), bookId);
        return Map.of("liked", on);
    }

    @PostMapping("/books/{id}/favorite/toggle")
    public Map<String, Object> toggleFavorite(@PathVariable("id") long bookId) {
        boolean on = xuehaiService.toggleFavorite(securityUtil.currentUserId(), bookId);
        return Map.of("favorited", on);
    }

    @PutMapping("/progress")
    public XuehaiProgressDto saveProgress(@RequestBody XuehaiProgressRequest req) {
        return xuehaiService.saveProgress(
                securityUtil.currentUserId(), req.bookId(), req.chapterId(), req.offset());
    }

    @GetMapping("/progress/{bookId}")
    public XuehaiProgressDto getProgress(@PathVariable long bookId) {
        return xuehaiService.getProgress(securityUtil.currentUserId(), bookId);
    }

    @GetMapping("/history")
    public List<XuehaiHistoryItemDto> history() {
        return xuehaiService.listHistory(securityUtil.currentUserId());
    }

    @DeleteMapping("/history/{id}")
    public Map<String, String> deleteHistory(@PathVariable long id) {
        xuehaiService.deleteHistory(securityUtil.currentUserId(), id);
        return Map.of("ok", "true");
    }

    @DeleteMapping("/history")
    public Map<String, String> clearHistory() {
        xuehaiService.clearHistory(securityUtil.currentUserId());
        return Map.of("ok", "true");
    }

    @GetMapping("/stats")
    public XuehaiStatsDto stats() {
        return xuehaiService.stats();
    }

    @GetMapping("/books/{id}/main-file")
    public ResponseEntity<Resource> downloadMain(@PathVariable("id") long bookId) throws IOException {
        xuehaiService.requireAuthForContent(securityUtil.currentUserId());
        Path path = xuehaiService.resolveMainFile(bookId);
        return fileResponse(path, xuehaiService.bookMainOriginalName(bookId));
    }

    @GetMapping("/books/{id}/main-file/inline")
    public ResponseEntity<Resource> inlineMain(@PathVariable("id") long bookId) throws IOException {
        xuehaiService.requireAuthForContent(securityUtil.currentUserId());
        Path path = xuehaiService.resolveMainFile(bookId);
        return inlineResponse(path, xuehaiService.bookMainOriginalName(bookId));
    }

    @GetMapping("/books/{id}/cover")
    public ResponseEntity<Resource> cover(@PathVariable("id") long bookId) throws IOException {
        Path path = xuehaiService.resolveCover(bookId);
        String probe = Files.probeContentType(path);
        MediaType mediaType =
                probe != null ? MediaType.parseMediaType(probe) : MediaType.APPLICATION_OCTET_STREAM;
        byte[] bytes = Files.readAllBytes(path);
        ByteArrayResource body = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(body);
    }

    @GetMapping("/chapters/{id}/file")
    public ResponseEntity<Resource> downloadChapter(@PathVariable("id") long chapterId)
            throws IOException {
        xuehaiService.requireAuthForContent(securityUtil.currentUserId());
        Path path = xuehaiService.resolveChapterFile(chapterId);
        return fileResponse(path, xuehaiService.chapterOriginalName(chapterId));
    }

    @GetMapping("/chapters/{id}/file/inline")
    public ResponseEntity<Resource> inlineChapter(@PathVariable("id") long chapterId)
            throws IOException {
        xuehaiService.requireAuthForContent(securityUtil.currentUserId());
        Path path = xuehaiService.resolveChapterFile(chapterId);
        return inlineResponse(path, xuehaiService.chapterOriginalName(chapterId));
    }

    private static ResponseEntity<Resource> fileResponse(Path path, String downloadName)
            throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        ByteArrayResource body = new ByteArrayResource(bytes);
        String encoded =
                URLEncoder.encode(downloadName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .body(body);
    }

    private static ResponseEntity<Resource> inlineResponse(Path path, String filename)
            throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        ByteArrayResource body = new ByteArrayResource(bytes);
        MediaType mediaType = resolveMediaType(path, filename);
        String encoded =
                URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encoded)
                .body(body);
    }

    private static MediaType resolveMediaType(Path path, String filename) throws IOException {
        String probe = Files.probeContentType(path);
        if (probe != null && !probe.isBlank()) {
            try {
                return MediaType.parseMediaType(probe);
            } catch (Exception ignored) {
                // fall through
            }
        }
        String lower = filename == null ? "" : filename.toLowerCase();
        int dot = lower.lastIndexOf('.');
        String ext = dot >= 0 ? lower.substring(dot + 1) : "";
        return switch (ext) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "txt" -> MediaType.parseMediaType("text/plain;charset=UTF-8");
            case "md", "markdown" -> MediaType.parseMediaType("text/markdown;charset=UTF-8");
            case "html", "htm" -> MediaType.parseMediaType("text/html;charset=UTF-8");
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
