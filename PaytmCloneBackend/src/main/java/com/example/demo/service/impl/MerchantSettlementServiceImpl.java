package com.example.demo.service.impl;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.LessAmountException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.factory.TransactionFactory;
import com.example.demo.mapper.SettlementMapper;
import com.example.demo.repository.BankAccountRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.service.MerchantSettlementService;
import com.example.demo.validation.KycValidator;

import jakarta.transaction.Transactional;

import com.example.demo.dto.MerchantSettlementTransactionDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MerchantSettlementServiceImpl implements MerchantSettlementService {
	
	private final TransactionRepository transactionRepository;
	private final WalletRepository walletRepository;
	private final SettlementMapper settlementMapper;
	private final BankAccountRepository bankAccountRepository;
	private final TransactionFactory transactionFactory;
	private final KycValidator kycValidator;
	
	public Page<MerchantSettlementTransactionDto> getSettlementHistory(AppUser user, Pageable pageable) throws ResourceNotFoundException{
		log.debug("Fetching settlement history for user ID: {}", user.getId());
		Wallet wallet = walletRepository.findByUser(user)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.WALLET_NOT_FOUND));

        return transactionRepository.findSettlementsByWallet(wallet, pageable)
                .map(settlementMapper::toDto); 
	}

	@Override
	@Transactional
	public void processInstantSettlement(AppUser merchant) throws UserNotFoundException, LessAmountException, ResourceNotFoundException {
		log.info("Starting instant settlement process for merchant: {}", merchant.getEmail());
		
		kycValidator.validateKycStatus(merchant);
		
		Wallet wallet = walletRepository.findByUser(merchant)
				.orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
		
		BigDecimal totalAmount = wallet.getBalance();
		
		// Minimum threshold for instant settlement
	    if (totalAmount.compareTo(new BigDecimal("100")) < 0) {
	        throw new LessAmountException(ErrorMessage.AMOUNT_LESS_THAN_100);
	    }
	    
	 // Calculate a 1% convenience fee
	    BigDecimal fee = totalAmount.multiply(new BigDecimal("0.01")); 
	    BigDecimal settleableAmount = totalAmount.subtract(fee);
	    
	    BankAccount primaryBank = bankAccountRepository.findByUserAndIsPrimaryTrueAndActiveTrue(merchant)
	    		.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PRIMARY_BANK_ACCOUNT_NOT_FOUND));
	    
	    log.info("Settling ₹{} to Bank Account: {} (ID: {})", settleableAmount, primaryBank.getAccountNumber(), primaryBank.getId());
	    
	    BigDecimal oldWalletBalance = wallet.getBalance();
		BigDecimal oldBankBalance = primaryBank.getBalance();
	    
	 // 1. Clear wallet
	    wallet.setBalance(BigDecimal.ZERO); 
	    
	    // 2. Add net amount to bank
	    primaryBank.setBalance(primaryBank.getBalance().add(settleableAmount));

	    // 3. Record Transaction
	    Transaction tx = transactionFactory.createSettlementTransaction(wallet, primaryBank, settleableAmount);
	    tx.setNarration("Instant Settlement (Fee: ₹" + fee + ")");
	    
	    transactionRepository.save(tx);
	    log.info("Settlement SUCCESS: Merchant: {} | From Wallet: ₹{} -> ₹0 | To Bank: ₹{} -> ₹{} | Tx ID: {}", 
				merchant.getEmail(), oldWalletBalance, oldBankBalance, primaryBank.getBalance(), tx.getId());
	}
}
