package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HighValueMerchantSummaryDto {
	private Long merchantId;
	private String merchantCode;
	private String businessName;
	private String category;
	private Long highValueTxnCount;
	private BigDecimal totalHighValueAmount;
	private Long distinctPayers;
}
