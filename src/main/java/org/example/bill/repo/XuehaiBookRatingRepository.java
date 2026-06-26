package org.example.bill.repo;

import java.util.List;
import java.util.Optional;
import org.example.bill.domain.XuehaiBookRating;
import org.example.bill.domain.XuehaiUserBookKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface XuehaiBookRatingRepository extends JpaRepository<XuehaiBookRating, XuehaiUserBookKey> {

    Optional<XuehaiBookRating> findByIdUserIdAndIdBookId(Long userId, Long bookId);

    @Query(
            "SELECT COALESCE(AVG(r.score), 0), COUNT(r) FROM XuehaiBookRating r WHERE r.id.bookId = :bookId")
    List<Object[]> aggregateByBookId(Long bookId);
}
