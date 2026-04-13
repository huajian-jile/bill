package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.BkpWechatBillTransaction;

@Mapper
public interface BkpWechatBillTransactionMapper extends BaseMapper<BkpWechatBillTransaction> {

    default Optional<BkpWechatBillTransaction> findByBillChannelAndSourceTxId(
            String billChannel, Long sourceTxId) {
        return Optional.ofNullable(
                selectOne(
                        Wrappers.<BkpWechatBillTransaction>lambdaQuery()
                                .eq(BkpWechatBillTransaction::getBillChannel, billChannel)
                                .eq(BkpWechatBillTransaction::getSourceTxId, sourceTxId)));
    }

    default List<BkpWechatBillTransaction> findByBillImportId(Long billImportId) {
        return selectList(
                Wrappers.<BkpWechatBillTransaction>lambdaQuery()
                        .eq(BkpWechatBillTransaction::getBillImportId, billImportId));
    }
}
