package org.example.bill.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.example.bill.domain.PhoneNumber;
import org.example.bill.mapper.PhoneNumberMapper;
import org.example.bill.web.dto.PhoneOptionDto;
import org.springframework.stereotype.Service;

/** 当前账号已绑定号码对应的 {@link PhoneNumber}（用于分析与下拉选项）。 */
@Service
@RequiredArgsConstructor
public class UserBillPhoneService {

    private final UserPhoneService userPhoneService;
    private final PhoneNumberMapper phoneNumberMapper;

    public Set<Long> allowedPhoneIds(long userId) {
        LinkedHashSet<Long> out = new LinkedHashSet<>();
        for (String mobile : userPhoneService.listMobiles(userId)) {
            PhoneNumber pn =
                    phoneNumberMapper.selectOne(
                            Wrappers.<PhoneNumber>lambdaQuery()
                                    .eq(PhoneNumber::getMobileCn, mobile));
            if (pn != null) {
                out.add(pn.getId());
            }
        }
        return out;
    }

    public List<PhoneOptionDto> listOptions(long userId) {
        List<PhoneOptionDto> out = new ArrayList<>();
        for (String mobile : userPhoneService.listMobiles(userId)) {
            PhoneNumber pn =
                    phoneNumberMapper.selectOne(
                            Wrappers.<PhoneNumber>lambdaQuery()
                                    .eq(PhoneNumber::getMobileCn, mobile));
            if (pn != null) {
                out.add(new PhoneOptionDto(pn.getId(), pn.getMobileCn()));
            }
        }
        return out;
    }

    /** 管理员：分析/导入等场景可选系统中全部号码（phone_number）。 */
    public List<PhoneOptionDto> listAllPhoneOptions() {
        return phoneNumberMapper
                .selectList(
                        Wrappers.<PhoneNumber>lambdaQuery()
                                .orderByAsc(PhoneNumber::getMobileCn))
                .stream()
                .map(p -> new PhoneOptionDto(p.getId(), p.getMobileCn()))
                .toList();
    }
}
