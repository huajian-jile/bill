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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "wechat_bill_transactions")
@TableName("wechat_bill_transactions")
@Getter
@Setter
@NoArgsConstructor
public class WechatBillTransaction {

    @TableId
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TableField("channel")
    @Column(name = "channel", nullable = false, length = 16)
    private String channel = "WECHAT";

    @TableField("bill_import_id")
    @Column(name = "bill_import_id", nullable = false)
    private Long billImportId;

    @TableField("person_id")
    @Column(name = "person_id")
    private Long personId;

    @TableField("phone_id")
    @Column(name = "phone_id")
    private Long phoneId;

    @TableField("mobile_cn")
    @Column(name = "mobile_cn", length = 11)
    private String mobileCn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @TableField("row_hash")
    @Column(name = "row_hash", nullable = false, length = 64)
    private String rowHash;

    @TableField("trade_time")
    private String tradeTime;

    @TableField("trade_type")
    private String tradeType;

    @TableField("counterparty")
    private String counterparty;

    @TableField("product")
    private String product;

    @TableField("income_expense")
    private String incomeExpense;

    @TableField("amount_yuan")
    private BigDecimal amountYuan;

    @TableField("payment_method")
    private String paymentMethod;

    @TableField("status")
    private String status;

    @TableField("trade_no")
    private String tradeNo;

    @TableField("merchant_no")
    private String merchantNo;

    @TableField("remark")
    private String remark;

    @TableField("source_file")
    private String sourceFile;

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
