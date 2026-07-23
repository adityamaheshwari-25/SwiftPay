package com.example.demo.dto;

import java.math.BigDecimal;

import com.example.demo.entity.enums.PaymentMode;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AddMoneyRequestDto {
	
	@NotNull(message = "Bank account is required")
	private Long bankAccountId;
	
	@NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1")
	private BigDecimal amount;
	
	@NotNull(message = "Payment mode is required")
	private PaymentMode paymentMode;
}
