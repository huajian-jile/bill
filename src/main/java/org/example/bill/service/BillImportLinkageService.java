package org.example.bill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.Person;
import org.example.bill.domain.PhoneNumber;
import org.example.bill.domain.WechatUser;
import org.example.bill.domain.PersonPhone;
import org.example.bill.mapper.PersonMapper;
import org.example.bill.mapper.PersonPhoneMapper;
import org.example.bill.mapper.PhoneNumberMapper;
import org.example.bill.mapper.WechatUserMapper;
import org.example.bill.util.PhoneUtil;
import org.springframework.stereotype.Service;

/**
 * 导入账单时：按手机号写入 {@code phone_number}，必要时新建 {@code person}，并回写 {@link WechatUser}
 * 的 {@code phone_id} / {@code person_id}，使导入批次与明细上的 person/phone 外键有值。
 *
 * <p>微信/支付宝共用 wechat_users 表，以 channel (WECHAT/ALIPAY) 区分。
 */
@Service
@RequiredArgsConstructor
public class BillImportLinkageService {

    private final PhoneNumberMapper phoneNumberMapper;
    private final PersonMapper personMapper;
    private final PersonPhoneMapper personPhoneMapper;
    private final WechatUserMapper wechatUserMapper;

    /**
     * 若该手机号已在 {@code phone_number} 中存在且已有对应 channel 的用户绑定，则返回该用户（按 id 最早一条），否则 empty。
     */
    public Optional<WechatUser> findUserAlreadyLinkedToMobile(String mobileCn, String channel) {
        String mobile = PhoneUtil.normalizeCnMobile(mobileCn);
        PhoneNumber pn =
                phoneNumberMapper.selectOne(
                        Wrappers.<PhoneNumber>lambdaQuery()
                                .eq(PhoneNumber::getMobileCn, mobile));
        if (pn == null) {
            return Optional.empty();
        }
        return wechatUserMapper.findFirstByPhoneIdAndChannel(pn.getId(), channel);
    }

    /**
     * 仅保证 {@code phone_number} 中存在该手机号（用户绑定/审核通过时调用）。<br>
     * 分析页下拉与 {@link UserBillPhoneService#listOptions} 依赖此表；导入账单时再补 person / wechat 关联。
     */
    public void ensurePhoneNumberRow(String mobileCn) {
        String mobile = PhoneUtil.normalizeCnMobile(mobileCn);
        PhoneNumber existing =
                phoneNumberMapper.selectOne(
                        Wrappers.<PhoneNumber>lambdaQuery()
                                .eq(PhoneNumber::getMobileCn, mobile));
        if (existing != null) {
            return;
        }
        PhoneNumber pn = new PhoneNumber();
        pn.setMobileCn(mobile);
        pn.setCreatedAt(Instant.now());
        phoneNumberMapper.insert(pn);
    }

    /**
     * 保证手机号在 {@code phone_number} 中存在，必要时新建 {@code person}，并回写 {@link WechatUser}
     * 的 {@code phone_id} / {@code person_id}。
     */
    public void ensurePhoneAndPersonLinked(WechatUser wu, String mobileCn) {
        String mobile = PhoneUtil.normalizeCnMobile(mobileCn);
        PhoneNumber pn =
                phoneNumberMapper.selectOne(
                        Wrappers.<PhoneNumber>lambdaQuery()
                                .eq(PhoneNumber::getMobileCn, mobile));
        if (pn == null) {
            pn = new PhoneNumber();
            pn.setMobileCn(mobile);
            pn.setCreatedAt(Instant.now());
            phoneNumberMapper.insert(pn);
        }
        boolean changed = false;
        if (wu.getPhoneId() == null || !wu.getPhoneId().equals(pn.getId())) {
            wu.setPhoneId(pn.getId());
            changed = true;
        }
        if (wu.getPersonId() == null) {
            Person p = new Person();
            p.setCreatedAt(Instant.now());
            p.setPhoneId(pn.getId());
            personMapper.insert(p);
            wu.setPersonId(p.getId());
            changed = true;
        } else {
            Person p = personMapper.selectById(wu.getPersonId());
            if (p != null && p.getPhoneId() == null) {
                p.setPhoneId(pn.getId());
                personMapper.updateById(p);
            }
        }
        if (changed) {
            wu.setUpdatedAt(Instant.now());
            wechatUserMapper.updateById(wu);
        }
        if (wu.getPersonId() != null && wu.getPhoneId() != null
                && !personPhoneMapper.existsPair(wu.getPersonId(), wu.getPhoneId())) {
            PersonPhone pp = new PersonPhone();
            pp.setPersonId(wu.getPersonId());
            pp.setPhoneId(wu.getPhoneId());
            pp.setCreatedAt(Instant.now());
            personPhoneMapper.insert(pp);
        }
    }
}
