package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class WalletResponseDto {
	private BigDecimal balance;
	private LocalDateTime updatedAt;
}
