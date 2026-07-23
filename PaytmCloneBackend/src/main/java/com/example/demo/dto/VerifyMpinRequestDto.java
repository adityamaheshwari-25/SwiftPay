package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyMpinRequestDto {
	@NotBlank
    @Pattern(regexp = "\\d{4}")
    private String mpin;
}
