package org.example.bill.repo;

import java.util.List;
import java.util.Optional;
import org.example.bill.domain.XuehaiBookshelf;
import org.example.bill.domain.XuehaiUserBookKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface XuehaiBookshelfRepository extends JpaRepository<XuehaiBookshelf, XuehaiUserBookKey> {

    List<XuehaiBookshelf> findByIdUserIdOrderByPinnedDescPinOrderAscLastReadAtDescAddedAtDesc(
            Long userId);

    long countByIdUserIdAndPinnedTrue(Long userId);

    boolean existsByIdUserIdAndIdBookId(Long userId, Long bookId);

    Optional<XuehaiBookshelf> findByIdUserIdAndIdBookId(Long userId, Long bookId);

    void deleteByIdUserIdAndIdBookId(Long userId, Long bookId);

    @Query("SELECT s.id.bookId FROM XuehaiBookshelf s WHERE s.id.userId = :userId")
    List<Long> findBookIdsByUserId(Long userId);
}
