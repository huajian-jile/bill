package org.example.bill.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bkp_wechat_bill_transactions")
@Getter
@Setter
@NoArgsConstructor
public class BkpWechatBillTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 对应原始导入明细；可空表示手工新增 */
    @Column(name = "source_tx_id", unique = true)
    private Long sourceTxId;

    @Column(name = "bill_import_id", nullable = false)
    private Long billImportId;

    /** WECHAT 或 ALIPAY */
    @Column(name = "bill_channel", nullable = false, length = 16)
    private String billChannel = "WECHAT";

    @Column(name = "row_hash", length = 64)
    private String rowHash;

    @Column(name = "trade_time")
    private String tradeTime;

    @Column(name = "trade_type")
    private String tradeType;

    private String counterparty;

    private String product;

    @Column(name = "income_expense")
    private String incomeExpense;

    @Column(name = "amount_yuan")
    private BigDecimal amountYuan;

    @Column(name = "payment_method")
    private String paymentMethod;

    private String status;

    @Column(name = "trade_no")
    private String tradeNo;

    @Column(name = "merchant_no")
    private String merchantNo;

    private String remark;

    @Column(name = "source_file")
    private String sourceFile;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    private String createdBy;
    private String updatedBy;

    @Column(name = "extra_text")
    private String extraText;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "app_user_id")
    private Long appUserId;
}
