package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BankAccountResponseDto {
	private Long bankAccountId;
	private String bankName;
	private String maskedAccountNumber;
	private String ifsc;
	private BigDecimal balance;
	private boolean verified;
	
	@JsonProperty("isPrimary")
	private boolean isPrimary;
	
	private LocalDateTime createdAt;
}
