package org.example.bill.repo;

import java.util.Optional;
import org.example.bill.domain.WechatUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WechatUserRepository extends JpaRepository<WechatUser, Long> {
    Optional<WechatUser> findFirstByWechatNicknameAndChannelOrderByIdAsc(String nickname, String channel);
}
