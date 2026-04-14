package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wechat_bill_imports")
@TableName("wechat_bill_imports")
@Getter
@Setter
@NoArgsConstructor
public class WechatBillImport {

    @TableId
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TableField("channel")
    @Column(name = "channel", nullable = false, length = 16)
    private String channel = "WECHAT";

    @TableField("user_id")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @TableField("person_id")
    @Column(name = "person_id")
    private Long personId;

    @TableField("phone_id")
    @Column(name = "phone_id")
    private Long phoneId;

    @TableField("mobile_cn")
    @Column(name = "mobile_cn", length = 11)
    private String mobileCn;

    @TableField("source_file")
    @Column(name = "source_file", nullable = false)
    private String sourceFile;

    @TableField("export_type")
    private String exportType;

    @TableField("export_time")
    private Instant exportTime;

    @TableField("range_start")
    private Instant rangeStart;

    @TableField("range_end")
    private Instant rangeEnd;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("income_count")
    private Integer incomeCount;

    @TableField("income_amount")
    private BigDecimal incomeAmount;

    @TableField("expense_count")
    private Integer expenseCount;

    @TableField("expense_amount")
    private BigDecimal expenseAmount;

    @TableField("neutral_count")
    private Integer neutralCount;

    @TableField("neutral_amount")
    private BigDecimal neutralAmount;

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
