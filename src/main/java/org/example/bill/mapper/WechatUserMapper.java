package org.example.bill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.example.bill.domain.WechatUser;

@Mapper
public interface WechatUserMapper extends BaseMapper<WechatUser> {

    default Optional<WechatUser> findFirstByWechatNicknameAndChannel(String nickname, String channel) {
        if (nickname == null || channel == null) {
            return Optional.empty();
        }
        List<WechatUser> list =
                selectList(
                        Wrappers.<WechatUser>lambdaQuery()
                                .eq(WechatUser::getWechatNickname, nickname)
                                .eq(WechatUser::getChannel, channel)
                                .orderByAsc(WechatUser::getId)
                                .last("LIMIT 1"));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /** 同一手机号应对应同一自然人；按已绑定的 phone_id + channel 找到最早一条用户。 */
    default Optional<WechatUser> findFirstByPhoneIdAndChannel(Long phoneId, String channel) {
        if (phoneId == null || channel == null) {
            return Optional.empty();
        }
        List<WechatUser> list =
                selectList(
                        Wrappers.<WechatUser>lambdaQuery()
                                .eq(WechatUser::getPhoneId, phoneId)
                                .eq(WechatUser::getChannel, channel)
                                .orderByAsc(WechatUser::getId)
                                .last("LIMIT 1"));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
