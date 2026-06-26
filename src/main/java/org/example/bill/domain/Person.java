package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@TableName("person")
public class Person {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属人展示名，可空；未设置时界面可用手机号代替。 */
    @TableField("display_name")
    private String displayName;

    /** 主归属号码，与 {@code phone_number.id} 一致，与微信侧 {@code wechat_users.phone_id} 对齐。 */
    @TableField("phone_id")
    private Long phoneId;

    @TableField("created_at")
    private Instant createdAt;
}
