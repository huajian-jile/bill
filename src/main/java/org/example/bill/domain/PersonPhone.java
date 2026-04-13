package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 所属人与手机号（phone_number）多对多关联 */
@Getter
@Setter
@NoArgsConstructor
@TableName("person_phone")
public class PersonPhone {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("person_id")
    private Long personId;

    @TableField("phone_id")
    private Long phoneId;

    @TableField("created_at")
    private Instant createdAt;
}
