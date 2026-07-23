package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.CreateBankAccountDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedAccessException;
import com.example.demo.exception.UserNotFoundException;

public interface BankAccountService {
	BankAccountResponseDto createBankAccount(String userEmail, CreateBankAccountDto dto) throws UserNotFoundException;
	List<BankAccountResponseDto> getMyBankAccounts(String userEmail) throws UserNotFoundException;
	void setPrimaryAccount(String userEmail, Long accountId) throws UserNotFoundException, ResourceNotFoundException, UnauthorizedAccessException;
}
