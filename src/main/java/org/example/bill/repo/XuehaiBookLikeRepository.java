package org.example.bill.repo;

import org.example.bill.domain.XuehaiBookLike;
import org.example.bill.domain.XuehaiUserBookKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface XuehaiBookLikeRepository extends JpaRepository<XuehaiBookLike, XuehaiUserBookKey> {

    boolean existsByIdUserIdAndIdBookId(Long userId, Long bookId);

    void deleteByIdUserIdAndIdBookId(Long userId, Long bookId);

    @Query("SELECT COUNT(l) FROM XuehaiBookLike l WHERE l.id.bookId = :bookId")
    long countForBook(@Param("bookId") Long bookId);
}
