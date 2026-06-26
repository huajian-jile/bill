package org.example.bill.repo;

import java.util.List;
import org.example.bill.domain.XuehaiChapter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XuehaiChapterRepository extends JpaRepository<XuehaiChapter, Long> {

    List<XuehaiChapter> findByBookIdOrderBySortOrderAscIdAsc(Long bookId);

    long countByBookId(Long bookId);
}
