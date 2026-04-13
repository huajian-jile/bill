package org.example.bill.domain;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "phone_id")
    private Long phoneId;

    @Column(name = "mobile_cn", length = 11)
    private String mobileCn;

    @Column(name = "source_file", nullable = false)
    private String sourceFile;

    @Column(name = "export_type")
    private String exportType;

    @Column(name = "export_time")
    private Instant exportTime;

    @Column(name = "range_start")
    private Instant rangeStart;

    @Column(name = "range_end")
    private Instant rangeEnd;

    @Column(name = "total_count")
    private Integer totalCount;

    @Column(name = "income_count")
    private Integer incomeCount;

    @Column(name = "income_amount")
    private BigDecimal incomeAmount;

    @Column(name = "expense_count")
    private Integer expenseCount;

    @Column(name = "expense_amount")
    private BigDecimal expenseAmount;

    @Column(name = "neutral_count")
    private Integer neutralCount;

    @Column(name = "neutral_amount")
    private BigDecimal neutralAmount;

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
