package org.example.bill.web.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BkpTransactionRequest(
        Long sourceTxId,
        @NotNull Long billImportId,
        /** WECHAT / ALIPAY，默认 WECHAT */
        String billChannel,
        String rowHash,
        String tradeTime,
        String tradeType,
        String counterparty,
        String product,
        String incomeExpense,
        BigDecimal amountYuan,
        String paymentMethod,
        String status,
        String tradeNo,
        String merchantNo,
        String remark,
        String sourceFile,
        String extraText,
        Boolean archived) {}
