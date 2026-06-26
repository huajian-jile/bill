package org.example.bill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.example.bill.web.dto.PersonPhoneLinkDto;
import org.example.bill.web.dto.PhoneOptionDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 所属人与 {@link org.example.bill.domain.PhoneNumber} 多对多绑定；仅允许在「当前账号账单可见」的号码与所属人之间操作。
 */
@Service
@RequiredArgsConstructor
public class PersonPhoneLinkService {

    private final PersonPhoneMapper personPhoneMapper;
    private final PersonMapper personMapper;
    private final PhoneNumberMapper phoneNumberMapper;
    private final WechatBillImportMapper wechatBillImportMapper;
    private final WechatUserMapper wechatUserMapper;
    private final PersonBillOptionsService personBillOptionsService;

    /** 当前登录手机号下曾出现过的账单号码（含登录号本身），可参与绑定 */
    public List<PhoneOptionDto> listLinkablePhones(String loginMobileCn) {
        String m = PhoneUtil.normalizeCnMobile(loginMobileCn);
        Map<Long, String> byId = new LinkedHashMap<>();
        PhoneNumber self = phoneNumber(m);
        if (self != null) {
            byId.put(self.getId(), self.getMobileCn());
        }
        for (WechatBillImport imp :
                wechatBillImportMapper.selectList(
                        Wrappers.<WechatBillImport>lambdaQuery()
                                .eq(WechatBillImport::getMobileCn, m))) {
            WechatUser wu = wechatUserMapper.selectById(imp.getUserId());
            if (wu == null || wu.getPhoneId() == null) {
                continue;
            }
            PhoneNumber pn = phoneNumberMapper.selectById(wu.getPhoneId());
            if (pn != null) {
                byId.putIfAbsent(pn.getId(), pn.getMobileCn());
            }
        }
        List<PhoneOptionDto> out = new ArrayList<>();
        for (Map.Entry<Long, String> e : byId.entrySet()) {
            out.add(new PhoneOptionDto(e.getKey(), e.getValue()));
        }
        out.sort(Comparator.comparing(PhoneOptionDto::mobileCn));
        return out;
    }

    public List<PersonPhoneLinkDto> listLinks(String loginMobileCn) {
        Set<Long> allowedPhoneIds = new LinkedHashSet<>();
        for (PhoneOptionDto p : listLinkablePhones(loginMobileCn)) {
            allowedPhoneIds.add(p.id());
        }
        if (allowedPhoneIds.isEmpty()) {
            return List.of();
        }
        Set<Long> allowedPersonIds = new LinkedHashSet<>();
        personBillOptionsService.listForMobile(loginMobileCn).forEach(o -> allowedPersonIds.add(o.id()));

        List<PersonPhoneLinkDto> out = new ArrayList<>();
        for (Long phoneId : allowedPhoneIds) {
            for (PersonPhone pp : personPhoneMapper.findByPhoneId(phoneId)) {
                if (!allowedPersonIds.contains(pp.getPersonId())) {
                    continue;
                }
                Person p = personMapper.selectById(pp.getPersonId());
                PhoneNumber pn = phoneNumberMapper.selectById(phoneId);
                if (p == null || pn == null) {
                    continue;
                }
                out.add(
                        new PersonPhoneLinkDto(
                                pp.getId(),
                                p.getId(),
                                label(p),
                                pn.getId(),
                                pn.getMobileCn()));
            }
        }
        out.sort(
                Comparator.comparing(PersonPhoneLinkDto::mobileCn)
                        .thenComparing(PersonPhoneLinkDto::personLabel));
        return out;
    }

    @Transactional
    public void addLink(String loginMobileCn, long personId, long phoneId) {
        requireAllowedPerson(loginMobileCn, personId);
        requireAllowedPhone(loginMobileCn, phoneId);
        if (personPhoneMapper.existsPair(personId, phoneId)) {
            return;
        }
        PersonPhone row = new PersonPhone();
        row.setPersonId(personId);
        row.setPhoneId(phoneId);
        row.setCreatedAt(Instant.now());
        personPhoneMapper.insert(row);
    }

    @Transactional
    public void removeLink(String loginMobileCn, long linkId) {
        PersonPhone pp = personPhoneMapper.selectById(linkId);
        if (pp == null) {
            throw new IllegalArgumentException("关联不存在");
        }
        requireAllowedPerson(loginMobileCn, pp.getPersonId());
        requireAllowedPhone(loginMobileCn, pp.getPhoneId());
        personPhoneMapper.deleteById(linkId);
    }

    private void requireAllowedPerson(String loginMobileCn, long personId) {
        boolean ok =
                personBillOptionsService.listForMobile(loginMobileCn).stream()
                        .anyMatch(o -> o.id() == personId);
        if (!ok) {
            throw new IllegalArgumentException("无权操作该所属人");
        }
    }

    private void requireAllowedPhone(String loginMobileCn, long phoneId) {
        boolean ok =
                listLinkablePhones(loginMobileCn).stream().anyMatch(p -> p.id() == phoneId);
        if (!ok) {
            throw new IllegalArgumentException("无权操作该号码");
        }
    }

    private PhoneNumber phoneNumber(String mobile) {
        return phoneNumberMapper.selectOne(
                Wrappers.<PhoneNumber>lambdaQuery().eq(PhoneNumber::getMobileCn, mobile));
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
