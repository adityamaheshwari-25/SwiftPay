package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

// noted one point in the request one we only uses @Data for getters and setters
// but in response one, we can also use Constructor one for returning the dto.

@Data
@AllArgsConstructor
public class WalletTransferResponseDto {
	private String txId;
	private BigDecimal amountTransferred;
    private BigDecimal remainingBalance;
	private LocalDateTime timestamp;
}
