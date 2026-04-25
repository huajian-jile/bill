package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.example.bill.domain.WechatBillTransaction;

@Mapper
public interface WechatBillTransactionMapper extends BaseMapper<WechatBillTransaction> {

    default Optional<WechatBillTransaction> findByPersonIdAndRowHash(Long personId, String rowHash) {
        return Optional.ofNullable(
                selectOne(
                        Wrappers.<WechatBillTransaction>lambdaQuery()
                                .eq(WechatBillTransaction::getPersonId, personId)
                                .eq(WechatBillTransaction::getRowHash, rowHash)));
    }

    default Optional<WechatBillTransaction> findByMobileCnAndRowHash(String mobileCn, String rowHash) {
        return Optional.ofNullable(
                selectOne(
                        Wrappers.<WechatBillTransaction>lambdaQuery()
                                .eq(WechatBillTransaction::getMobileCn, mobileCn)
                                .eq(WechatBillTransaction::getRowHash, rowHash)));
    }

    default List<WechatBillTransaction> findByBillImportIdIn(Collection<Long> importIds) {
        if (importIds == null || importIds.isEmpty()) {
            return List.of();
        }
        return selectList(
                Wrappers.<WechatBillTransaction>lambdaQuery()
                        .in(WechatBillTransaction::getBillImportId, importIds));
    }

    @Select(
            """
            SELECT t.* FROM bill_import_data t
            WHERE t.bill_import_id IN (SELECT id FROM bill_import_record WHERE user_id = #{wechatUserId})
            """)
    List<WechatBillTransaction> selectAllByWechatUserId(@Param("wechatUserId") Long wechatUserId);

    @Select(
            """
            SELECT t.* FROM bill_import_data t
            WHERE t.bill_import_id IN (SELECT id FROM bill_import_record WHERE user_id = #{wechatUserId})
            AND t.is_archived = false
            """)
    List<WechatBillTransaction> selectActiveByWechatUserId(@Param("wechatUserId") Long wechatUserId);

    List<WechatBillTransaction> selectActiveByWechatUserIds(@Param("userIds") Collection<Long> userIds);

    List<WechatBillTransaction> selectActiveByPersonIds(@Param("personIds") Collection<Long> personIds);

    List<WechatBillTransaction> selectActiveByPhoneIds(@Param("phoneIds") Collection<Long> phoneIds);
}
