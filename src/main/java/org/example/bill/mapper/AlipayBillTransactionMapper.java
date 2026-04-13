package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.bill.domain.AlipayBillTransaction;

@Mapper
public interface AlipayBillTransactionMapper extends BaseMapper<AlipayBillTransaction> {

    default Optional<AlipayBillTransaction> findByPersonIdAndRowHash(Long personId, String rowHash) {
        return Optional.ofNullable(
                selectOne(
                        Wrappers.<AlipayBillTransaction>lambdaQuery()
                                .eq(AlipayBillTransaction::getPersonId, personId)
                                .eq(AlipayBillTransaction::getRowHash, rowHash)));
    }

    default Optional<AlipayBillTransaction> findByMobileCnAndRowHash(String mobileCn, String rowHash) {
        return Optional.ofNullable(
                selectOne(
                        Wrappers.<AlipayBillTransaction>lambdaQuery()
                                .eq(AlipayBillTransaction::getMobileCn, mobileCn)
                                .eq(AlipayBillTransaction::getRowHash, rowHash)));
    }

    List<AlipayBillTransaction> selectActiveByWechatUserIds(@Param("userIds") Collection<Long> userIds);

    List<AlipayBillTransaction> selectActiveByPersonIds(@Param("personIds") Collection<Long> personIds);

    List<AlipayBillTransaction> selectActiveByPhoneIds(@Param("phoneIds") Collection<Long> phoneIds);
}
