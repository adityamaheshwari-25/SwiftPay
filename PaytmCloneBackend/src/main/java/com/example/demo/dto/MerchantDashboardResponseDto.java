package com.example.demo.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MerchantDashboardResponseDto {
	private UserResponseDto profile;
	private CollectionStatsResponse stats;
	private SecurityStatusResponseDto security;
	private List<BankAccountResponseDto> bankAccounts;
}
