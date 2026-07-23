package com.example.demo.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WalletTransferRequestDto {
	
	@NotBlank
	@Pattern(regexp = "\\d{10}", message = "Invalid mobile number")
	private String receiverMobile;
	
	@NotNull
    @DecimalMin(value = "1.00", message = "Minimum amount is 1")
	private BigDecimal amount;
	
	@NotBlank
	private String mpin;
}
