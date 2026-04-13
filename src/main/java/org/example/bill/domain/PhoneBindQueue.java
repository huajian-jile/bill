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
@TableName("phone_bind_request")
public class PhoneBindQueue {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("mobile_cn")
    private String mobileCn;

    private String status;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("reviewed_at")
    private Instant reviewedAt;

    @TableField("reviewed_by_user_id")
    private Long reviewedByUserId;

    /** 仅 REJECTED 时有值；展示给申请人。 */
    @TableField("reject_reason")
    private String rejectReason;
}
