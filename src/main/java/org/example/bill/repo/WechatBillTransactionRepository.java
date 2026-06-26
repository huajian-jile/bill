package org.example.bill.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.bill.domain.WechatBillTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WechatBillTransactionRepository extends JpaRepository<WechatBillTransaction, Long> {

    Optional<WechatBillTransaction> findByPersonIdAndRowHash(Long personId, String rowHash);

    Optional<WechatBillTransaction> findByTradeNo(String tradeNo);

    List<WechatBillTransaction> findByBillImportIdIn(Collection<Long> importIds);

    long deleteByBillImportIdIn(Collection<Long> importIds);

    @Query(
            """
            SELECT t FROM WechatBillTransaction t
            WHERE t.billImportId IN (
              SELECT i.id FROM WechatBillImport i WHERE i.userId = :wechatUserId
            )
            """)
    List<WechatBillTransaction> findAllByWechatUserId(@Param("wechatUserId") Long wechatUserId);

    @Query(
            """
            SELECT t FROM WechatBillTransaction t
            WHERE t.billImportId IN (
              SELECT i.id FROM WechatBillImport i WHERE i.userId = :wechatUserId
            )
            AND t.archived = false
            """)
    List<WechatBillTransaction> findActiveByWechatUserId(@Param("wechatUserId") Long wechatUserId);

    @Query(
            """
            SELECT t FROM WechatBillTransaction t
            WHERE t.billImportId IN (
              SELECT i.id FROM WechatBillImport i WHERE i.userId IN :userIds
            )
            AND t.archived = false
            """)
    List<WechatBillTransaction> findActiveByWechatUserIds(@Param("userIds") Collection<Long> userIds);

    @Query(
            """
            SELECT t FROM WechatBillTransaction t
            WHERE t.channel = :channel
            AND t.archived = false
            """)
    List<WechatBillTransaction> findActiveByChannel(@Param("channel") String channel);
}
