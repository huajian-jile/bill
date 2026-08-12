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
@TableName("memo_category")
public class MemoCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("scope_key")
    private String scopeKey;

    @TableField("name")
    private String name;

    @TableField("sort_order")
    private Integer sortOrder;

    /** JSONB array of transaction IDs, stored as string in DB. */
    @TableField("tx_ids")
    private String txIds;

    @TableField("created_at")
    private Instant createdAt;

    @TableField("updated_at")
    private Instant updatedAt;
}
