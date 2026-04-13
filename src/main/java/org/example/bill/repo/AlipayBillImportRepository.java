package org.example.bill.repo;

import java.util.List;
import org.example.bill.domain.AlipayBillImport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlipayBillImportRepository extends JpaRepository<AlipayBillImport, Long> {
    List<AlipayBillImport> findByUserId(Long userId);
}
