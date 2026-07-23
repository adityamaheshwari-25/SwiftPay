package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.customAnnotation.Idempotent;
import com.example.demo.dto.AddMoneyRequestDto;
import com.example.demo.dto.SpendingInsightDto;
import com.example.demo.dto.WalletResponseDto;
import com.example.demo.dto.WalletTransferRequestDto;
import com.example.demo.dto.WalletTransferResponseDto;
import com.example.demo.dto.WithdrawMoneyRequestDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.enums.TransactionStatus;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.InActiveBankAccountException;
import com.example.demo.exception.InsufficientBalanceException;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.KycNotApprovedException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.NotVerifiedBankAccountException;
import com.example.demo.exception.ReceiverNotFoundException;
import com.example.demo.exception.ReceiverWalletNotFoundException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.SelfTransferNotAllowedException;
import com.example.demo.exception.SenderWalletNotFoundException;
import com.example.demo.exception.UnauthorizedAccessException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;
import com.example.demo.factory.TransactionFactory;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.BankAccountRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.KycValidationService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.WalletService;
import com.example.demo.validation.BankAccountValidator;
import com.example.demo.validation.WalletAmountValidator;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*	One succeeds
	One throws OptimisticLockException
	Balance is never corrupted
*/
/*
 * WalletServiceImpl
 * -----------------
 * Core business service responsible for all wallet operations:
 *  - Add money
 *  - Withdraw money
 *  - Wallet-to-wallet (P2P) transfers
 *  - Wallet balance view
 *  - Spending insights
 *
 * Concurrency Handling:
 *  - Optimistic locking using @Version on entities
 *  - Automatic retries on OptimisticLock exceptions
 *
 * Safety Guarantees:
 *  - Idempotent APIs
 *  - Atomic DB transactions
 *  - Balance integrity is never corrupted
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService{

    private final AppUserRepository appUserRepository;
	
	private final CurrentUserService currentUserService;
	private final WalletRepository walletRepository;
	private final BankAccountRepository bankAccountRepository;
	private final TransactionService transactionService;
	private final TransactionFactory transactionFactory;
	private final BankAccountValidator bankAccountValidator;
	private final BCryptPasswordEncoder passwordEncoder;
	private final WalletAmountValidator walletAmountValidator;
	private final KycValidationService kycValidationService;
	private final TransactionRepository transactionRepository;
	
	/**
     * Adds money from a user's bank account into their wallet.
     *
     * Guarantees:
     * - Idempotent operation
     * - Optimistic locking with retry
     * - Atomic balance update for bank & wallet
     */
	@Override
	@Idempotent(api = "ADD_MONEY") // enables aspect
	@Transactional // everything in this method happens in one DB transaction per attempt, if any runtime exception occurs, transaction rolls back
	@Retryable(
			retryFor = { ObjectOptimisticLockingFailureException.class },
			maxAttempts = 3,
			backoff = @Backoff(delay = 100, multiplier = 2)
	)
	public WalletResponseDto addMoney(Authentication authentication, AddMoneyRequestDto dto) throws UserNotFoundException, WalletNotFoundException, UnauthorizedAccessException, InActiveBankAccountException, NotVerifiedBankAccountException, InsufficientBalanceException, KycNotApprovedException, ResourceNotFoundException {
		AppUser user = currentUserService.getCurrentUser(authentication);
		
		log.info("Starting 'Add Money' for User: {}. Amount: ₹{}, BankID: {}", user.getEmail(), dto.getAmount(), dto.getBankAccountId());
		
		// Log retry attempts (useful for debugging contention issues)
		var context = RetrySynchronizationManager.getContext();
		int attempt = (context != null) ? context.getRetryCount() : 0;
		if (attempt > 0) {
			log.warn("Retry Attempt #{} for Add Money - User: {}", attempt, user.getEmail());
		}
		
		
		kycValidationService.ensureKycApproved(user);
		
		Wallet wallet = walletRepository.findByUser(user)
											.orElseThrow(() -> new WalletNotFoundException(ErrorMessage.WALLET_NOT_FOUND));
			
		BankAccount bankAccount = bankAccountRepository
									.findByIdAndUser(dto.getBankAccountId(), user)
									.orElseThrow(() -> {
										log.error("Unauthorized bank access attempt by user: {} for bank ID: {}", user.getEmail(), dto.getBankAccountId());										
										return new UnauthorizedAccessException(ErrorMessage.INVALID_BANK_ACCOUNT);
									});
		bankAccountValidator.validateForDebit(bankAccount, dto.getAmount());
			
        /*
         * Critical section:
         * - Both wallet & bank account balances are updated
         * - Optimistic locking ensures concurrency safety
         * 
         * When concurrent updates happen, Hibernate throws optimistic lock exception → triggers retry.
         */
		bankAccount.setBalance(bankAccount.getBalance().subtract(dto.getAmount()));
		wallet.setBalance(wallet.getBalance().add(dto.getAmount()));
			
		Transaction transaction = 
					transactionFactory
					.createAddMoneyTransaction(bankAccount, wallet, dto.getAmount(), dto.getPaymentMode());
			
		transactionService.save(transaction);
			
		log.info("Success: Added ₹{} to Wallet (ID: {}). New Balance: ₹{}", dto.getAmount(), wallet.getId(), wallet.getBalance());
			
		return new WalletResponseDto(wallet.getBalance(), wallet.getUpdatedAt());
	}
	
    /**
     * Withdraws money from wallet to user's bank account.
     *
     * Security:
     * - MPIN validation
     * - KYC enforcement
     */
	@Override
	@Idempotent(api = "WITHDRAW_MONEY")
	@Transactional
	@Retryable(
	        retryFor = { ObjectOptimisticLockingFailureException.class },
	        maxAttempts = 3,
	        backoff = @Backoff(delay = 100, multiplier = 2)
	    )
	public WalletResponseDto withdrawMoney(Authentication authentication, WithdrawMoneyRequestDto dto) throws UserNotFoundException, WalletNotFoundException, UnauthorizedAccessException, InsufficientBalanceException, MpinNotSetException, InvalidMpinException, KycNotApprovedException, ResourceNotFoundException {
		AppUser user = currentUserService.getCurrentUser(authentication);
		log.info("Starting 'Withdraw' for User: {}. Amount: ₹{}", user.getEmail(), dto.getAmount());
		
		kycValidationService.ensureKycApproved(user);
		
		if (!user.isMpinSet()) {
			throw new MpinNotSetException(ErrorMessage.SET_MPIN_BEFORE_WITHDRAWAL);
		}
			
		if (!passwordEncoder.matches(dto.getMpin(), user.getMpin())) {
			log.warn("Withdrawal failed: Invalid MPIN for user: {}", user.getEmail());
			throw new InvalidMpinException(ErrorMessage.INVALID_MPIN);
		}
			
		Wallet wallet = walletRepository.findByUser(user)
											.orElseThrow(() -> new WalletNotFoundException(ErrorMessage.WALLET_NOT_FOUND));
			
		BankAccount bankAccount = bankAccountRepository
									.findByIdAndUser(dto.getBankAccountId(), user)
									.orElseThrow(() -> new UnauthorizedAccessException(ErrorMessage.INVALID_BANK_ACCOUNT));
			
		// wallet balance validation
		if (wallet.getBalance().compareTo(dto.getAmount()) < 0) {
			log.warn("Withdrawal failed: Insufficient wallet balance for user: {}", user.getEmail());
			throw new InsufficientBalanceException(ErrorMessage.INSUFFICIENT_BALANCE);
		}
			
		// Update balances atomically
		wallet.setBalance(wallet.getBalance().subtract(dto.getAmount()));
		bankAccount.setBalance(bankAccount.getBalance().add(dto.getAmount()));

		Transaction transaction = transactionFactory.createWithdrawTransaction(wallet, bankAccount, dto.getAmount());
			
		transactionService.save(transaction);
			
		log.info("Success: Withdrew ₹{} to Bank (ID: {}). Remaining Wallet: ₹{}", dto.getAmount(), bankAccount.getId(), wallet.getBalance());
		
		return new WalletResponseDto(wallet.getBalance(), wallet.getUpdatedAt());
			
	}



    /**
     * Transfers money from one wallet to another (P2P).
     *
     * Validations:
     * - MPIN
     * - Self-transfer prevention
     * - Receiver existence
     */
	@Override
	@Idempotent(api = "WALLET_TRANSFER")
	@Transactional
	@Retryable(
	        retryFor = { ObjectOptimisticLockingFailureException.class },
	        maxAttempts = 3,
	        backoff = @Backoff(delay = 100, multiplier = 2)
	    )
	public WalletTransferResponseDto transferWalletToWallet(Authentication authentication,
			WalletTransferRequestDto dto) throws SenderWalletNotFoundException, ReceiverNotFoundException, SelfTransferNotAllowedException, MpinNotSetException, UserNotFoundException, InvalidMpinException, ReceiverWalletNotFoundException, InsufficientBalanceException, KycNotApprovedException, ResourceNotFoundException {
		
		AppUser sender = currentUserService.getCurrentUser(authentication);
		
		log.info("Initiating P2P Transfer. Sender: {}, Receiver Mobile: {}, Amount: ₹{}", sender.getEmail(), dto.getReceiverMobile(), dto.getAmount());
		
		kycValidationService.ensureKycApproved(sender);
		
		if (!sender.isMpinSet()) {
			throw new MpinNotSetException(ErrorMessage.SET_MPIN_BEFORE_WITHDRAWAL);
		}
		
		if (!passwordEncoder.matches(dto.getMpin(), sender.getMpin())) {
			log.warn("P2P Transfer failed: Invalid MPIN for sender: {}", sender.getEmail());
			throw new InvalidMpinException(ErrorMessage.INVALID_MPIN);
		}
		
		AppUser receiver = appUserRepository.findByMobile(dto.getReceiverMobile())
							.orElseThrow(() -> {
								log.warn("P2P Transfer failed: Receiver mobile {} not found", dto.getReceiverMobile());
								return new ReceiverNotFoundException(ErrorMessage.RECEIVER_NOT_FOUND);
							});
		if (sender.getId().equals(receiver.getId())) {
			throw new SelfTransferNotAllowedException(ErrorMessage.SELF_TRANSFER_NOT_ALLOWED);
		}
		
		Wallet senderWallet = walletRepository.findByUser(sender)
		        .orElseThrow(() ->
		                new SenderWalletNotFoundException(ErrorMessage.SENDER_WALLET_NOT_FOUND)
		        );

		Wallet receiverWallet = walletRepository.findByUser(receiver)
		        .orElseThrow(() ->
		                new ReceiverWalletNotFoundException(ErrorMessage.RECEIVER_WALLET_NOT_FOUND)
		        );

		walletAmountValidator.validateDebit(senderWallet, dto.getAmount());
		
		senderWallet.setBalance(senderWallet.getBalance().subtract(dto.getAmount()));
		receiverWallet.setBalance(receiverWallet.getBalance().add(dto.getAmount()));
		
		Transaction tx = 
					transactionFactory.createWalletTransferTransaction(
							senderWallet, 
							receiverWallet, 
							dto.getAmount()
		);
		
		transactionService.save(tx);
		
		log.info("P2P Transfer Success: TxID: {}, Sender: {}, Receiver: {}, Amount: ₹{}", tx.getTxId(), sender.getEmail(), receiver.getEmail(), dto.getAmount());
		
		return new WalletTransferResponseDto(
						tx.getTxId(), 
						dto.getAmount(), 
						senderWallet.getBalance(),
						LocalDateTime.now()	
					);
		
		
	}
	
    /**
     * Returns wallet balance for the authenticated user.
     */
	@Override
	public WalletResponseDto viewWallet(Authentication authentication) throws UserNotFoundException, WalletNotFoundException {
		AppUser user = currentUserService.getCurrentUser(authentication);
		log.debug("User {} viewing wallet balance", user.getEmail());
		
		Wallet wallet = walletRepository.findByUser(user)
				.orElseThrow(() -> new WalletNotFoundException(ErrorMessage.WALLET_NOT_FOUND));
		
		return new WalletResponseDto(wallet.getBalance(), wallet.getUpdatedAt());
		
	}

	/**
     * Generates spending insights comparing current and previous month.
     */
	public SpendingInsightDto getSpendingInsight(Authentication auth) throws UserNotFoundException, ResourceNotFoundException{
	    AppUser user = currentUserService.getCurrentUser(auth);
	    log.debug("Calculating spending insights for user: {}", user.getEmail());
	    
	    Wallet wallet = walletRepository.findByUser(user)
	            .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.WALLET_NOT_FOUND));

	    // Date Range Setup
	    LocalDateTime now = LocalDateTime.now();
	    LocalDateTime startOfCurrentMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
	    
	    LocalDateTime startOfLastMonth = startOfCurrentMonth.minusMonths(1);
	    LocalDateTime endOfLastMonth = startOfCurrentMonth.minusNanos(1);

	    // 1. Calculate Current Month Spending
	    BigDecimal currentSpent = transactionRepository.sumSpendingByWalletInRange(
	            wallet, TransactionStatus.SUCCESS, startOfCurrentMonth, now);

	    // 2. Calculate Last Month Spending
	    BigDecimal lastSpent = transactionRepository.sumSpendingByWalletInRange(
	            wallet, TransactionStatus.SUCCESS, startOfLastMonth, endOfLastMonth);

	    // Handle null values from DB
	    currentSpent = (currentSpent == null) ? BigDecimal.ZERO : currentSpent;
	    lastSpent = (lastSpent == null) ? BigDecimal.ZERO : lastSpent;

	    // 3. Logic for Percentage Change
	    double percentageChange = 0;
	    boolean isIncrease = false;

	    if (lastSpent.compareTo(BigDecimal.ZERO) > 0) {
	        BigDecimal difference = currentSpent.subtract(lastSpent);
	        // Calculate absolute percentage change
	        percentageChange = Math.abs((difference.doubleValue() / lastSpent.doubleValue()) * 100);
	        isIncrease = currentSpent.compareTo(lastSpent) > 0;
	    } else if (currentSpent.compareTo(BigDecimal.ZERO) > 0) {
	        // If they spent 0 last month but something this month, it's 100% increase
	        percentageChange = 100.0;
	        isIncrease = true;
	    }

	    return new SpendingInsightDto(
	            currentSpent,
	            lastSpent,
	            percentageChange,
	            isIncrease
	    );
	}
	
	/**
     * Recovery method invoked when retry attempts are exhausted.
     */
	@Recover
	public WalletResponseDto recoverWalletAction(ObjectOptimisticLockingFailureException e, Authentication auth, Object dto) {
		log.error("Exhausted retries for {} due to high database contention. User: {}", dto.getClass().getSimpleName(), auth.getName());
        throw e; // Delegate to GlobalExceptionHandler
	}
	
	
	
	
}
