package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.PaymentMode;
import com.example.demo.entity.enums.TransactionStatus;
import com.example.demo.entity.enums.TransactionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserTransactionResponseDto {
    private String txId;           // Internal unique ID
    private String referenceId;    // External/Bank reference (UTR)
    
    // The "Who"
    private String counterPartyName; // Name of the person/merchant you dealt with
    
    private TransactionType transactionType;
    private PaymentMode paymentMode;

    private BigDecimal amount;
    private TransactionStatus status;
    private String failureReason;  // To show on UI if status is FAILED

    private String narration;
    private LocalDateTime createdAt;

    // UI helper: Instead of boolean, maybe an enum for RED/GREEN colors
    private boolean isCredit; 
}
