package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class KycApproveRequestDto {
	@NotNull
	private Long userId;
}
