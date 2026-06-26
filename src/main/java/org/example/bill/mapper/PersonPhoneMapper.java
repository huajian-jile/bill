package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.PersonPhone;

@Mapper
public interface PersonPhoneMapper extends BaseMapper<PersonPhone> {

    default List<PersonPhone> findByPhoneId(Long phoneId) {
        if (phoneId == null) {
            return List.of();
        }
        return selectList(
                Wrappers.<PersonPhone>lambdaQuery().eq(PersonPhone::getPhoneId, phoneId));
    }

    default List<PersonPhone> findByPersonId(Long personId) {
        if (personId == null) {
            return List.of();
        }
        return selectList(
                Wrappers.<PersonPhone>lambdaQuery().eq(PersonPhone::getPersonId, personId));
    }

    default boolean existsPair(Long personId, Long phoneId) {
        return selectCount(
                        Wrappers.<PersonPhone>lambdaQuery()
                                .eq(PersonPhone::getPersonId, personId)
                                .eq(PersonPhone::getPhoneId, phoneId))
                > 0;
    }
}
