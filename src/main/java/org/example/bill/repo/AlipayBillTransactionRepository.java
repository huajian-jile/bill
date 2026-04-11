package org.example.bill.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.bill.domain.AlipayBillTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlipayBillTransactionRepository extends JpaRepository<AlipayBillTransaction, Long> {

    Optional<AlipayBillTransaction> findByPersonIdAndRowHash(Long personId, String rowHash);

    @Query(
            """
            SELECT t FROM AlipayBillTransaction t
            WHERE t.billImportId IN (
              SELECT i.id FROM AlipayBillImport i WHERE i.userId IN :userIds
            )
            AND t.archived = false
            """)
    List<AlipayBillTransaction> findActiveByUserIds(@Param("userIds") Collection<Long> userIds);
}
