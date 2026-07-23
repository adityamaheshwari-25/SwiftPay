package com.example.demo.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.CreateBankAccountDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;
import com.example.demo.util.MaskingUtil;

@Component
public class BankAccountMapper {
	
	public BankAccount toEntity(CreateBankAccountDto dto, AppUser user) {
		BankAccount account = new BankAccount();
		account.setUser(user);
		account.setBankName(dto.getBankName());
		account.setAccountNumber(dto.getAccountNumber());
		account.setIfsc(dto.getIfsc());
		return account;
	}
	
	public BankAccountResponseDto toResponseDto(BankAccount entity) {
		return new BankAccountResponseDto(
					entity.getId(),
					entity.getBankName(),
					MaskingUtil.maskAccountNumber(entity.getAccountNumber()),
					entity.getIfsc(),
					entity.getBalance(),
					entity.isVerified(),
					entity.isPrimary(),
					entity.getCreatedAt()
				);
	}
}
