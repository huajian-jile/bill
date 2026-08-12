package org.example.bill.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.MemoCategory;
import org.example.bill.mapper.MemoCategoryMapper;
import org.example.bill.web.SecurityUtil;
import org.example.bill.web.dto.MemoCategoryDto;
import org.example.bill.web.dto.MemoCategorySaveRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MemoCategoryService {

    private final MemoCategoryMapper mapper;
    private final SecurityUtil securityUtil;
    private final ObjectMapper objectMapper;

    private Long requireUserId() {
        Long uid = securityUtil.currentUserId();
        if (uid == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return uid;
    }

    private Long[] parseTxIds(String json) {
        if (json == null || json.isBlank()) {
            return new Long[0];
        }
        try {
            List<Long> list = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return list.toArray(new Long[0]);
        } catch (JsonProcessingException e) {
            return new Long[0];
        }
    }

    private String toJson(Long[] ids) {
        try {
            return objectMapper.writeValueAsString(ids);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private MemoCategoryDto toDto(MemoCategory entity) {
        return new MemoCategoryDto(
                entity.getId(),
                entity.getName(),
                entity.getSortOrder() != null ? entity.getSortOrder() : 0,
                parseTxIds(entity.getTxIds()));
    }

    public List<MemoCategoryDto> list(String scopeKey) {
        Long userId = requireUserId();
        List<MemoCategory> list = mapper.findByUserIdAndScopeKey(userId, scopeKey);
        return list.stream().map(this::toDto).toList();
    }

    @Transactional
    public void save(MemoCategorySaveRequest req) {
        Long userId = requireUserId();
        String scopeKey = req.scopeKey();
        if (scopeKey == null || scopeKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scopeKey 不能为空");
        }

        mapper.deleteByUserIdAndScopeKey(userId, scopeKey);

        Instant now = Instant.now();
        int idx = 0;
        for (MemoCategorySaveRequest.CategoryItem item : req.categories()) {
            MemoCategory entity = new MemoCategory();
            entity.setUserId(userId);
            entity.setScopeKey(scopeKey);
            entity.setName(item.name() != null ? item.name() : "未命名");
            entity.setSortOrder(item.sortOrder());
            entity.setTxIds(toJson(item.txIds() != null ? item.txIds() : new Long[0]));
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            mapper.insert(entity);
            idx++;
        }
    }

    @Transactional
    public void clear(String scopeKey) {
        Long userId = requireUserId();
        mapper.deleteByUserIdAndScopeKey(userId, scopeKey);
    }
}
