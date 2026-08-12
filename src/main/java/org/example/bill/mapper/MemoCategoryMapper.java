package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.MemoCategory;

@Mapper
public interface MemoCategoryMapper extends BaseMapper<MemoCategory> {

    default List<MemoCategory> findByUserIdAndScopeKey(Long userId, String scopeKey) {
        if (userId == null || scopeKey == null) {
            return List.of();
        }
        return selectList(
                Wrappers.<MemoCategory>lambdaQuery()
                        .eq(MemoCategory::getUserId, userId)
                        .eq(MemoCategory::getScopeKey, scopeKey)
                        .orderByAsc(MemoCategory::getSortOrder));
    }

    default void deleteByUserIdAndScopeKey(Long userId, String scopeKey) {
        if (userId == null || scopeKey == null) {
            return;
        }
        delete(
                Wrappers.<MemoCategory>lambdaQuery()
                        .eq(MemoCategory::getUserId, userId)
                        .eq(MemoCategory::getScopeKey, scopeKey));
    }
}
