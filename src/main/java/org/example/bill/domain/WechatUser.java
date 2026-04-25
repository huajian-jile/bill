package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bill_users")
@TableName("bill_users")
@Getter
@Setter
@NoArgsConstructor
public class WechatUser {

    @TableId
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TableField("channel")
    @Column(name = "channel", nullable = false, length = 16)
    private String channel = "WECHAT";

    @TableField("wechat_nickname")
    @Column(name = "wechat_nickname", nullable = false)
    private String wechatNickname;

    @TableField("person_id")
    @Column(name = "person_id")
    private Long personId;

    @TableField("phone_id")
    @Column(name = "phone_id")
    private Long phoneId;

    @TableField("created_at")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @TableField("updated_at")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("extra_text")
    private String extraText;

    @TableField("is_archived")
    @Column(name = "is_archived", nullable = false)
    private Boolean archived = false;
}
