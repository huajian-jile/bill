package org.example.bill.repo;

import java.time.Instant;
import java.util.List;
import org.example.bill.domain.XuehaiBook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XuehaiBookRepository extends JpaRepository<XuehaiBook, Long> {

    List<XuehaiBook> findAllByOrderByCreatedAtDesc();

    List<XuehaiBook> findTop10ByOrderByLikeCountDescIdDesc();

    List<XuehaiBook> findTop10ByOrderByFavoriteCountDescIdDesc();

    long countByMainFileStoragePathIsNotNull();

    @Query(
            value =
                    "SELECT date_trunc('month', created_at) AS bucket, COUNT(*)::bigint AS cnt "
                            + "FROM xuehai_book GROUP BY 1 ORDER BY 1",
            nativeQuery = true)
    List<Object[]> countGroupedByMonth();

    @Query(
            "SELECT b FROM XuehaiBook b WHERE "
                    + "(:contentType IS NULL OR b.contentType = :contentType) AND "
                    + "(:status IS NULL OR b.status = :status) AND "
                    + "(:keyword IS NULL OR :keyword = '' OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
                    + "OR LOWER(COALESCE(b.author, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<XuehaiBook> search(
            @Param("contentType") String contentType,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(
            "SELECT b FROM XuehaiBook b ORDER BY b.recommendWeight DESC, b.readCount DESC, b.avgScore DESC, b.id DESC")
    Page<XuehaiBook> rankRecommend(Pageable pageable);

    @Query(
            "SELECT b FROM XuehaiBook b WHERE b.status = 'finished' ORDER BY b.readCount DESC, b.id DESC")
    Page<XuehaiBook> rankFinished(Pageable pageable);

    @Query(
            "SELECT b FROM XuehaiBook b WHERE b.publishAt >= :since ORDER BY b.publishAt DESC, b.id DESC")
    Page<XuehaiBook> rankNewBook(@Param("since") Instant since, Pageable pageable);

    List<XuehaiBook> findTop6ByContentTypeAndIdNotOrderByReadCountDesc(
            String contentType, Long excludeId);

    List<XuehaiBook> findTop6ByIdNotOrderByReadCountDesc(Long excludeId);
}
