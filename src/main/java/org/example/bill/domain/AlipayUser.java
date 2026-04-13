package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 支付宝账号维度，归属 {@link Person}；同一所属人可有多个支付宝账号（多条记录）。 */
@Getter
@Setter
@NoArgsConstructor
@TableName("alipay_users")
public class AlipayUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("person_id")
    private Long personId;

    /** 手机号，与 {@code phone_number.mobile_cn} 对齐 */
    @TableField("mobile_cn")
    private String mobileCn;

    @TableField("alipay_nickname")
    private String alipayNickname;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;

    @TableField("extra_text")
    private String extraText;

    @TableField("is_archived")
    private Boolean archived;
}
