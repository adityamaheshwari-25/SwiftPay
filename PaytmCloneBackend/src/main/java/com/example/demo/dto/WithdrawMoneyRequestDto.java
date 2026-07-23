package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WithdrawMoneyRequestDto {
	
	@NotNull(message = "Bank account is required")
	private Long bankAccountId;
	
	@NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1")
	private BigDecimal amount;
	
	@NotBlank(message = "MPIN is required")
    private String mpin;
}
