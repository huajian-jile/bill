package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wechat_users")
@TableName("wechat_users")
@Getter
@Setter
@NoArgsConstructor
public class WechatUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wechat_nickname", nullable = false)
    private String wechatNickname;

    /** 与 Python 导入的 person / 自然人 对齐 */
    @Column(name = "person_id")
    private Long personId;

    @Column(name = "phone_id")
    private Long phoneId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;

    @Column(name = "extra_text")
    private String extraText;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
