package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycRejectRequestDto {
	@NotBlank
	private String rejectionReason;
}
