package org.example.bill.repo;

import java.util.List;
import java.util.Optional;
import org.example.bill.domain.BkpWechatBillTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BkpWechatBillTransactionRepository
        extends JpaRepository<BkpWechatBillTransaction, Long>,
                JpaSpecificationExecutor<BkpWechatBillTransaction> {

    List<BkpWechatBillTransaction> findByBillImportId(Long billImportId);

    Optional<BkpWechatBillTransaction> findByBillChannelAndSourceTxId(String billChannel, Long sourceTxId);
}
