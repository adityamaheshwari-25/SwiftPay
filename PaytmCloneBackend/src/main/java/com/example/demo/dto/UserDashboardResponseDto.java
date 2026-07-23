package com.example.demo.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDashboardResponseDto {
	private UserResponseDto profile;
	private WalletResponseDto wallet;
	private List<BankAccountResponseDto> bankAccounts;
	private Page<UserTransactionResponseDto> transactions;
	private SecurityStatusResponseDto security;
	private SpendingInsightDto spending;
}
