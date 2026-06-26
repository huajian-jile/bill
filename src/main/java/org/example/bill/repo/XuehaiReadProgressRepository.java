package org.example.bill.repo;

import java.util.List;
import java.util.Optional;
import org.example.bill.domain.XuehaiReadProgress;
import org.example.bill.domain.XuehaiUserBookKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XuehaiReadProgressRepository extends JpaRepository<XuehaiReadProgress, XuehaiUserBookKey> {

    Optional<XuehaiReadProgress> findByIdUserIdAndIdBookId(Long userId, Long bookId);

    List<XuehaiReadProgress> findByIdUserIdOrderByUpdatedAtDesc(Long userId);
}
