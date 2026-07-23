package com.example.demo.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.demo.dto.MerchantTransactionResponseDto;
import com.example.demo.dto.UserTransactionResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Transaction;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.MerchantTransactionMapper;
import com.example.demo.mapper.UserTransactionMapper;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService{

	private final TransactionRepository transactionRepository;
	private final UserTransactionMapper userTransactionMapper;
	private final CurrentUserService currentUserService;
	private final MerchantTransactionMapper merchantTransactionMapper;
	
	@Override
	public Transaction save(Transaction transaction) {
		log.info("Persisting new transaction: Type={}, Amount={}", 
				transaction.getTransactionType(), transaction.getAmount());
		Transaction savedTx = transactionRepository.save(transaction);
		log.debug("Transaction saved successfully with ID: {}", savedTx.getId());
		return savedTx;
	}

	public Page<UserTransactionResponseDto> getMyTransactions(Authentication auth, Pageable pageable) throws UserNotFoundException {
        AppUser user = currentUserService.getCurrentUser(auth);
        
        log.debug("Fetching user transactions for User: {} [Page: {}, Size: {}]", 
				user.getEmail(), pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Transaction> txPage = transactionRepository.findUserTransactions(user.getId(), pageable);
		
		log.info("Retrieved {} transactions for user: {}", txPage.getNumberOfElements(), user.getEmail());
		
		return txPage.map(tx -> userTransactionMapper.mapToDto(tx, user));
    }

	@Override
	public Page<MerchantTransactionResponseDto> getMerchantTransactions(Authentication authentication, Pageable pageable) throws UserNotFoundException{
		AppUser user = currentUserService.getCurrentUser(authentication);
		
		log.debug("Fetching merchant collection history for: {} [Page: {}]", 
				user.getEmail(), pageable.getPageNumber());
		
		// 2. Fetch transactions where this user is the receiver
        Page<Transaction> txPage = transactionRepository.findMerchantTransactions(user.getId(), pageable);
		
        log.info("Found {} merchant transactions for: {}", txPage.getNumberOfElements(), user.getEmail());
        
		return txPage.map(tx -> merchantTransactionMapper.mapToMerchantDto(tx));
	}
	
	
	
}
