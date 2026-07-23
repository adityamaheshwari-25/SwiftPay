package com.example.demo.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.factory.TransactionFactory;
import com.example.demo.repository.BankAccountRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.validation.KycValidator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementService {
	
	private final WalletRepository walletRepository;
	private final BankAccountRepository bankAccountRepository;
	private final TransactionRepository transactionRepository;
	private final TransactionFactory transactionFactory;
	private final KycValidator kycValidator;
	
	@Transactional
	public void processSettlement(AppUser merchant) throws UserNotFoundException{
		kycValidator.validateKycStatus(merchant);
		
		Wallet wallet = walletRepository.findByUser(merchant)
				.orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
		
		BigDecimal amount = wallet.getBalance();
		
		// 1. Check if there is money to settle (Best practice: set a min threshold like ₹10)
        if (amount.compareTo(BigDecimal.TEN) < 0) {
            log.info("Settlement skipped for {}: Balance below threshold", merchant.getEmail());
            return;
        }
		
     // 2. Find Primary Bank Account (using your new primary logic)
        BankAccount primaryBank = bankAccountRepository
                .findByUserAndIsPrimaryTrueAndActiveTrue(merchant)
                .orElseGet(() -> {
                    log.warn("No primary bank for merchant: {}", merchant.getEmail());
                    return null;
                });
        
        if (primaryBank == null) return;
        
     // 3. Update Balances
        wallet.setBalance(wallet.getBalance().subtract(amount));
        primaryBank.setBalance(primaryBank.getBalance().add(amount));
        
     // 4. Use Factory to create record
        Transaction settlementTx = transactionFactory.createSettlementTransaction(
            wallet, 
            primaryBank, 
            amount
        );
        
     // 5. Persist
        transactionRepository.save(settlementTx);
        log.info("Settled ₹{} to bank {} for merchant {}", 
                 amount, primaryBank.getAccountNumber(), merchant.getEmail());
		
	}
}
