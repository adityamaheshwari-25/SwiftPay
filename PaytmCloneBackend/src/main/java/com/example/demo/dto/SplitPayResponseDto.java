package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SplitPayResponseDto {
	private Long splitId;
	private String txId;
	private BigDecimal paidAmount;
	private String message;
}
