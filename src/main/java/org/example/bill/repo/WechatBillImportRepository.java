package org.example.bill.repo;

import java.util.List;
import java.util.Optional;
import org.example.bill.domain.WechatBillImport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WechatBillImportRepository extends JpaRepository<WechatBillImport, Long> {
    List<WechatBillImport> findByUserId(Long userId);

    Optional<WechatBillImport> findTopByMobileCnOrderByIdAsc(String mobileCn);

    long countByUserId(Long userId);
}
