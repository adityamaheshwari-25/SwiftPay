package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantHighValueTxnRowDto {
	private Long merchantId;
    private String merchantCode;
    private String businessName;
    private String category;

    private Long payerUserId;
    private String payerName;
    private String payerEmail;
    private String payerMobile;

    private Long transactionDbId;
    private String txId;
    private String referenceId;
    private String transactionType;
    private String paymentMode;
    private BigDecimal amount;
    private String status;
    private LocalDateTime createdAt;
}
