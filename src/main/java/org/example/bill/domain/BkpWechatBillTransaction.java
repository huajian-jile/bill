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
@Table(name = "bkp_wechat_bill_transactions")
@TableName("bkp_wechat_bill_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BkpWechatBillTransaction {

    @TableId
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TableField("source_tx_id")
    @Column(name = "source_tx_id", unique = true)
    private Long sourceTxId;

    @TableField("bill_import_id")
    @Column(name = "bill_import_id", nullable = false)
    private Long billImportId;

    @TableField("bill_channel")
    @Column(name = "bill_channel", nullable = false, length = 16)
    private String billChannel = "WECHAT";

    @JdbcTypeCode(SqlTypes.CHAR)
    @TableField("row_hash")
    @Column(name = "row_hash", length = 64)
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
    private Instant createdAt = Instant.now();

    @TableField("updated_at")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @TableField("created_by")
    private String createdBy;

    @TableField("updated_by")
    private String updatedBy;

    @TableField("extra_text")
    private String extraText;

    @TableField("is_archived")
    @Column(name = "is_archived", nullable = false)
    private Boolean archived = false;

    @TableField("app_user_id")
    @Column(name = "app_user_id")
    private Long appUserId;
}
