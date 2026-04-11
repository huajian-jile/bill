package org.example.bill.repo;

import java.util.Optional;
import org.example.bill.domain.WechatUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WechatUserRepository extends JpaRepository<WechatUser, Long> {
    /** 昵称不再全局唯一；取最早一条以兼容旧逻辑 */
    Optional<WechatUser> findTopByWechatNicknameOrderByIdAsc(String nickname);
}
