package org.example.bill.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wechat_bill_transactions")
@TableName("wechat_bill_transactions")
@Getter
@Setter
@NoArgsConstructor
public class WechatBillTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_import_id", nullable = false)
    private Long billImportId;

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "phone_id")
    private Long phoneId;

    @Column(name = "mobile_cn", length = 11)
    private String mobileCn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "row_hash", nullable = false, length = 64)
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
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private String createdBy;
    private String updatedBy;

    @Column(name = "extra_text")
    private String extraText;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;
}
