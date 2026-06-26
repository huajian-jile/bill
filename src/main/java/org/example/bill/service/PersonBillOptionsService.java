package org.example.bill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.Person;
import org.example.bill.domain.PersonPhone;
import org.example.bill.domain.PhoneNumber;
import org.example.bill.domain.WechatBillImport;
import org.example.bill.domain.WechatUser;
import org.example.bill.mapper.PersonMapper;
import org.example.bill.mapper.PersonPhoneMapper;
import org.example.bill.mapper.PhoneNumberMapper;
import org.example.bill.mapper.WechatBillImportMapper;
import org.example.bill.mapper.WechatUserMapper;
import org.example.bill.util.PhoneUtil;
import org.example.bill.web.dto.PersonOptionDto;
import org.springframework.stereotype.Service;

/**
 * 与当前登录手机号相关的「所属人」列表：用于分析页按人筛选（非微信用户 id）。
 */
@Service
@RequiredArgsConstructor
public class PersonBillOptionsService {

    private final PersonMapper personMapper;
    private final PersonPhoneMapper personPhoneMapper;
    private final PhoneNumberMapper phoneNumberMapper;
    private final WechatBillImportMapper wechatBillImportMapper;
    private final WechatUserMapper wechatUserMapper;

    public List<PersonOptionDto> listForMobile(String mobileCn) {
        String m = PhoneUtil.normalizeCnMobile(mobileCn);
        PhoneNumber pn =
                phoneNumberMapper.selectOne(
                        Wrappers.<PhoneNumber>lambdaQuery().eq(PhoneNumber::getMobileCn, m));
        Set<Long> seen = new LinkedHashSet<>();
        List<PersonOptionDto> out = new ArrayList<>();
        if (pn != null) {
            for (Person p :
                    personMapper.selectList(
                            Wrappers.<Person>lambdaQuery().eq(Person::getPhoneId, pn.getId()))) {
                if (seen.add(p.getId())) {
                    out.add(new PersonOptionDto(p.getId(), label(p)));
                }
            }
            for (WechatUser wu :
                    wechatUserMapper.selectList(
                            Wrappers.<WechatUser>lambdaQuery().eq(WechatUser::getPhoneId, pn.getId()))) {
                if (wu.getPersonId() != null && seen.add(wu.getPersonId())) {
                    Person p = personMapper.selectById(wu.getPersonId());
                    if (p != null) {
                        out.add(new PersonOptionDto(p.getId(), label(p)));
                    }
                }
            }
        }
        Set<Long> scopePhoneIds = new LinkedHashSet<>();
        if (pn != null) {
            scopePhoneIds.add(pn.getId());
        }
        for (WechatBillImport imp :
                wechatBillImportMapper.selectList(
                        Wrappers.<WechatBillImport>lambdaQuery()
                                .eq(WechatBillImport::getMobileCn, m))) {
            WechatUser wu = wechatUserMapper.selectById(imp.getUserId());
            if (wu != null && wu.getPhoneId() != null) {
                scopePhoneIds.add(wu.getPhoneId());
            }
        }
        for (Long phoneId : scopePhoneIds) {
            for (PersonPhone pp : personPhoneMapper.findByPhoneId(phoneId)) {
                if (seen.add(pp.getPersonId())) {
                    Person p = personMapper.selectById(pp.getPersonId());
                    if (p != null) {
                        out.add(new PersonOptionDto(p.getId(), label(p)));
                    }
                }
            }
        }
        out.sort((a, b) -> Long.compare(a.id(), b.id()));
        return out;
    }

    public Long firstLinkedPersonId(String mobileCn) {
        List<PersonOptionDto> list = listForMobile(mobileCn);
        return list.isEmpty() ? null : list.get(0).id();
    }

    private String label(Person p) {
        if (p.getDisplayName() != null && !p.getDisplayName().isBlank()) {
            return p.getDisplayName().trim();
        }
        if (p.getPhoneId() != null) {
            PhoneNumber ph = phoneNumberMapper.selectById(p.getPhoneId());
            if (ph != null && ph.getMobileCn() != null) {
                return ph.getMobileCn();
            }
        }
        return "所属人#" + p.getId();
    }
}
