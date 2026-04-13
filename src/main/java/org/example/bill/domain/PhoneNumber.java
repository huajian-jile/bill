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
@TableName("phone_number")
public class PhoneNumber {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("mobile_cn")
    private String mobileCn;

    @TableField("created_at")
    private Instant createdAt;
}
