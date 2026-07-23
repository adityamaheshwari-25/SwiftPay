package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SetMpinRequestDto {
	
	@NotBlank(message = "MPIN is required")
    @Pattern(
        regexp = "\\d{4}",
        message = "MPIN must be exactly 4 digits"
    )
    private String mpin;

}
