package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.TransactionStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantSettlementTransactionDto {
	private String txId;
	private BigDecimal amount;      // Net amount received in bank
    private BigDecimal fee;         // The convenience fee charged
    private String destinationBankName;   // e.g., HDFC Bank
    private String accountNumberTail;      // e.g., ******4521
    private String utrNumber;              // The "Reference ID" from the bank
    private TransactionStatus status;      // PENDING, SUCCESS, FAILED
    private String narration;              // "Daily Settlement for 18th Oct"
    private LocalDateTime settledAt;
    private boolean isInstant;      // To show a "Flash" icon in the UI
}
