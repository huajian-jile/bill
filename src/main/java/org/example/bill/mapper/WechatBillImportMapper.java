package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.WechatBillImport;

@Mapper
public interface WechatBillImportMapper extends BaseMapper<WechatBillImport> {

    default List<WechatBillImport> findByUserId(Long userId) {
        return selectList(Wrappers.<WechatBillImport>lambdaQuery().eq(WechatBillImport::getUserId, userId));
    }

    default Optional<WechatBillImport> findTopByMobileCnOrderByIdAsc(String mobileCn) {
        List<WechatBillImport> list =
                selectList(
                        Wrappers.<WechatBillImport>lambdaQuery()
                                .eq(WechatBillImport::getMobileCn, mobileCn)
                                .orderByAsc(WechatBillImport::getId)
                                .last("LIMIT 1"));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
