package org.example.bill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.PhoneBindQueue;
import org.example.bill.mapper.PhoneBindQueueMapper;
import org.example.bill.util.PhoneUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PhoneBindQueueService {

    private final PhoneBindQueueMapper queueMapper;
    private final UserPhoneService userPhoneService;

    /** 与 {@link UserPhoneService} 使用的 app_user_phones 一致（勿再用 user_phone 表计数，否则会误判「首个」）。 */
    public long countBoundPhones(Long userId) {
        return userPhoneService.listMobiles(userId).size();
    }

    /**
     * 当前已绑定号码数为 0 时直接写入；否则进入待审核队列。
     *
     * @return {@code true} 已直接绑定；{@code false} 已提交审核
     */
    @Transactional
    public boolean requestBindOrApproveDirect(Long userId, String rawMobile) {
        PhoneUtil.requireValidCnMobile(rawMobile);
        String mobile = PhoneUtil.normalizeCnMobile(rawMobile);
        if (userPhoneService.listMobiles(userId).contains(mobile)) {
            throw new IllegalArgumentException("该号码已绑定");
        }
        if (queueMapper.selectCount(
                        Wrappers.<PhoneBindQueue>lambdaQuery()
                                .eq(PhoneBindQueue::getUserId, userId)
                                .eq(PhoneBindQueue::getMobileCn, mobile)
                                .eq(PhoneBindQueue::getStatus, PhoneBindQueue.STATUS_PENDING))
                > 0) {
            throw new IllegalArgumentException("该号码已在审核中");
        }
        if (countBoundPhones(userId) == 0) {
            userPhoneService.addPhone(userId, rawMobile);
            return true;
        }
        createPending(userId, mobile);
        return false;
    }

    @Transactional
    public void createPending(Long userId, String normalizedMobile) {
        if (userPhoneService.listMobiles(userId).contains(normalizedMobile)) {
            throw new IllegalArgumentException("该号码已绑定");
        }
        PhoneBindQueue row = new PhoneBindQueue();
        row.setUserId(userId);
        row.setMobileCn(normalizedMobile);
        row.setStatus(PhoneBindQueue.STATUS_PENDING);
        row.setCreatedAt(Instant.now());
        queueMapper.insert(row);
    }

    public List<PhoneBindQueue> listPendingForUser(Long userId) {
        return queueMapper.selectList(
                Wrappers.<PhoneBindQueue>lambdaQuery()
                        .eq(PhoneBindQueue::getUserId, userId)
                        .eq(PhoneBindQueue::getStatus, PhoneBindQueue.STATUS_PENDING)
                        .orderByDesc(PhoneBindQueue::getCreatedAt));
    }

    public List<PhoneBindQueue> listPendingAll() {
        return queueMapper.selectList(
                Wrappers.<PhoneBindQueue>lambdaQuery()
                        .eq(PhoneBindQueue::getStatus, PhoneBindQueue.STATUS_PENDING)
                        .orderByAsc(PhoneBindQueue::getCreatedAt));
    }

    /** 已处理（通过或拒绝）的申请，按审核时间倒序。 */
    public List<PhoneBindQueue> listProcessedHistory() {
        return queueMapper.selectList(
                Wrappers.<PhoneBindQueue>lambdaQuery()
                        .in(
                                PhoneBindQueue::getStatus,
                                PhoneBindQueue.STATUS_APPROVED,
                                PhoneBindQueue.STATUS_REJECTED)
                        .orderByDesc(PhoneBindQueue::getReviewedAt));
    }

    /** 当前用户全部绑定申请（含待审），按申请时间倒序。 */
    public List<PhoneBindQueue> listAllForUser(Long userId) {
        return queueMapper.selectList(
                Wrappers.<PhoneBindQueue>lambdaQuery()
                        .eq(PhoneBindQueue::getUserId, userId)
                        .orderByDesc(PhoneBindQueue::getCreatedAt));
    }

    @Transactional
    public void approve(Long id, Long reviewerUserId) {
        PhoneBindQueue row = queueMapper.selectById(id);
        if (row == null || !PhoneBindQueue.STATUS_PENDING.equals(row.getStatus())) {
            throw new IllegalArgumentException("记录不存在或已处理");
        }
        userPhoneService.addPhone(row.getUserId(), row.getMobileCn());
        row.setStatus(PhoneBindQueue.STATUS_APPROVED);
        row.setReviewedAt(Instant.now());
        row.setReviewedByUserId(reviewerUserId);
        row.setRejectReason(null);
        queueMapper.updateById(row);
    }

    @Transactional
    public void reject(Long id, Long reviewerUserId, String reason) {
        String r = reason == null ? "" : reason.trim();
        if (r.isEmpty()) {
            throw new IllegalArgumentException("请填写拒绝理由");
        }
        if (r.length() > 500) {
            throw new IllegalArgumentException("拒绝理由不超过 500 字");
        }
        PhoneBindQueue row = queueMapper.selectById(id);
        if (row == null || !PhoneBindQueue.STATUS_PENDING.equals(row.getStatus())) {
            throw new IllegalArgumentException("记录不存在或已处理");
        }
        row.setStatus(PhoneBindQueue.STATUS_REJECTED);
        row.setReviewedAt(Instant.now());
        row.setReviewedByUserId(reviewerUserId);
        row.setRejectReason(r);
        queueMapper.updateById(row);
    }
}
