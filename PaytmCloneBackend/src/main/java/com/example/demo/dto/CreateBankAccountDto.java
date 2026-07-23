package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBankAccountDto {
	
	@NotBlank(message = "Bank name is required")
	private String bankName;
	
	@NotBlank(message = "Account number is required")
    @Size(min = 9, max = 18, message = "Account number must be between 9 and 18 digits")
    @Pattern(regexp = "\\d+", message = "Account number must contain only digits")
	private String accountNumber;
	
	@NotBlank(message = "IFSC code is required")
    @Pattern(
        regexp = "^[A-Z]{4}0[A-Z0-9]{6}$",
        message = "Invalid IFSC code format"
    )
	private String ifsc;
}
