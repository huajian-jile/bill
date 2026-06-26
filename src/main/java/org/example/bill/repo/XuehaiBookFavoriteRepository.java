package org.example.bill.repo;

import java.util.List;
import org.example.bill.domain.XuehaiBookFavorite;
import org.example.bill.domain.XuehaiUserBookKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XuehaiBookFavoriteRepository extends JpaRepository<XuehaiBookFavorite, XuehaiUserBookKey> {

    boolean existsByIdUserIdAndIdBookId(Long userId, Long bookId);

    void deleteByIdUserIdAndIdBookId(Long userId, Long bookId);

    List<XuehaiBookFavorite> findByIdUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(f) FROM XuehaiBookFavorite f WHERE f.id.bookId = :bookId")
    long countForBook(@Param("bookId") Long bookId);
}
