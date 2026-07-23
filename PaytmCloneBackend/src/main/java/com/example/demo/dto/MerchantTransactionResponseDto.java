package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.PaymentMode;
import com.example.demo.entity.enums.TransactionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantTransactionResponseDto {
    private String txId;
    private String customerName; // The business needs to know who paid
    private BigDecimal amount;
    private TransactionStatus status;
    private PaymentMode paymentMode;
    private String narration;
    private LocalDateTime createdAt;
    private String referenceId; // Crucial for merchant support/reconciliation
}
