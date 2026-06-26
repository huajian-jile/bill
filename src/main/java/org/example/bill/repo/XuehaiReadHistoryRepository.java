package org.example.bill.repo;

import java.util.List;
import org.example.bill.domain.XuehaiReadHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface XuehaiReadHistoryRepository extends JpaRepository<XuehaiReadHistory, Long> {

    List<XuehaiReadHistory> findByUserIdOrderByReadAtDesc(Long userId, org.springframework.data.domain.Pageable pageable);

    long countByUserId(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("DELETE FROM XuehaiReadHistory h WHERE h.userId = :userId")
    void deleteAllByUserId(Long userId);

    @Query(
            value =
                    "SELECT id FROM xuehai_read_history WHERE user_id = :userId ORDER BY read_at DESC OFFSET 200",
            nativeQuery = true)
    List<Long> findIdsBeyondLimit(Long userId);
}
