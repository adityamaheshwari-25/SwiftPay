package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import com.example.demo.dto.MerchantTransactionResponseDto;
import com.example.demo.dto.UserTransactionResponseDto;
import com.example.demo.entity.Transaction;
import com.example.demo.exception.UserNotFoundException;

public interface TransactionService {
	Transaction save(Transaction transaction);
	
	Page<UserTransactionResponseDto> getMyTransactions(
	        Authentication authentication,
	        Pageable pageable 
	) throws UserNotFoundException;
	
	Page<MerchantTransactionResponseDto> getMerchantTransactions(Authentication authentication, Pageable pageable) throws UserNotFoundException;

	
}
