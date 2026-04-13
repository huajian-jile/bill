package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.AlipayUser;

@Mapper
public interface AlipayUserMapper extends BaseMapper<AlipayUser> {

    default Optional<AlipayUser> findByPersonIdAndAlipayNickname(Long personId, String alipayNickname) {
        if (personId == null || alipayNickname == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(
                selectOne(
                        Wrappers.<AlipayUser>lambdaQuery()
                                .eq(AlipayUser::getPersonId, personId)
                                .eq(AlipayUser::getAlipayNickname, alipayNickname)
                                .last("LIMIT 1")));
    }

    /**
     * 按手机号查找归属的 AlipayUser（通过 person_phone → person → alipay_users）。
     * 可能返回多条（同一自然人注册过多个支付宝账号），取第一条。
     */
    default Optional<AlipayUser> findFirstByMobileCn(String mobileCn) {
        List<AlipayUser> list =
                selectList(
                        Wrappers.<AlipayUser>lambdaQuery()
                                .eq(AlipayUser::getMobileCn, mobileCn)
                                .orderByAsc(AlipayUser::getId)
                                .last("LIMIT 1"));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 根据一批手机号（phone_number.mobile_cn）查找对应的 AlipayUser ID。
     * 用于分析接口按手机号筛选支付宝数据。
     */
    default List<Long> findIdsByMobileCns(Collection<String> mobileCns) {
        if (mobileCns == null || mobileCns.isEmpty()) {
            return List.of();
        }
        return selectList(
                        Wrappers.<AlipayUser>lambdaQuery()
                                .select(AlipayUser::getId)
                                .in(AlipayUser::getMobileCn, mobileCns))
                .stream()
                .map(AlipayUser::getId)
                .toList();
    }
}
