package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AddMoneyRequestDto;
import com.example.demo.dto.SpendingInsightDto;
import com.example.demo.dto.WalletResponseDto;
import com.example.demo.dto.WalletTransferRequestDto;
import com.example.demo.dto.WalletTransferResponseDto;
import com.example.demo.dto.WithdrawMoneyRequestDto;
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
import com.example.demo.service.WalletService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WalletController
 * ----------------
 * Core financial REST controller responsible for:
 *  - Adding money to wallet
 *  - Withdrawing money to bank
 *  - Wallet-to-wallet (P2P) transfers
 *  - Viewing wallet balance
 *  - Fetching spending insights
 *
 * Notes:
 * - All monetary operations support idempotency to prevent double processing.
 * - Authentication context is used to identify the current user.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {
	
	private final WalletService walletService;
	
	
	/**
     * Adds funds to the user's wallet from a linked bank account.
     *
     * Security & Reliability:
     * - Requires Idempotency-Key header to prevent duplicate transactions
     * - Validates KYC, bank account status, and sufficient balance
     *
     * @param key Idempotency key provided by client
     * @param authentication Authenticated user context
     * @param dto Add money request payload
     * 
     * @return Updated wallet balance
     * 
     * @throws UserNotFoundException if the authenticated user does not exist
	 * @throws WalletNotFoundException if wallet is not found for the user
	 * @throws UnauthorizedAccessException if bank account does not belong to the user
	 * @throws InActiveBankAccountException if bank account is inactive
	 * @throws NotVerifiedBankAccountException if bank account is not verified
	 * @throws InsufficientBalanceException if bank account balance is insufficient
	 * @throws KycNotApprovedException if user's KYC is not approved
	 * @throws ResourceNotFoundException if required resources are missing
     */
	@PostMapping("/add-money")
	public ResponseEntity<WalletResponseDto> addMoney(
				@RequestHeader("Idempotency-Key") String key,
				Authentication authentication,
				@Valid @RequestBody AddMoneyRequestDto dto
			) throws UserNotFoundException, WalletNotFoundException, UnauthorizedAccessException, InActiveBankAccountException, NotVerifiedBankAccountException, InsufficientBalanceException, KycNotApprovedException, ResourceNotFoundException {
		log.info("REST request to add money: User={}, Amount={}, Key={}", 
				authentication.getName(), dto.getAmount(), key);
		WalletResponseDto response = walletService.addMoney(authentication, dto);
		log.info("Money added successfully for user {}. New Balance: {}", 
				authentication.getName(), response.getBalance());
		return ResponseEntity.ok(response);
	}
	
	/**
     * Withdraws funds from the user's wallet to a linked bank account.
     *
     * Security:
     * - MPIN verification
     * - KYC approval required
     * - Idempotent operation
     *
     * @param key Idempotency key provided by client
     * @param authentication Authenticated user context
     * @param dto Withdrawal request payload
     * 
     * @return Updated wallet balance
     * 
     * @throws UserNotFoundException if user does not exist
	 * @throws WalletNotFoundException if wallet is not found
	 * @throws UnauthorizedAccessException if bank account access is invalid
	 * @throws InsufficientBalanceException if wallet has insufficient funds
	 * @throws MpinNotSetException if MPIN is not set
	 * @throws InvalidMpinException if MPIN verification fails
	 * @throws KycNotApprovedException if KYC is not approved
	 * @throws ResourceNotFoundException if bank account or wallet is missing
     */
	@PostMapping("/withdraw")
    public ResponseEntity<WalletResponseDto> withdrawMoney(
    		@RequestHeader("Idempotency-Key") String key,
            Authentication authentication,
            @Valid @RequestBody WithdrawMoneyRequestDto dto
    ) throws UserNotFoundException, WalletNotFoundException, UnauthorizedAccessException, InsufficientBalanceException, MpinNotSetException, InvalidMpinException, KycNotApprovedException, ResourceNotFoundException {
		log.info("REST request to withdraw money: User={}, Amount={}, Key={}", 
				authentication.getName(), dto.getAmount(), key);
        WalletResponseDto response =
                walletService.withdrawMoney(authentication, dto);
        log.info("Withdrawal successful for user {}. New Balance: {}", 
        		authentication.getName(), response.getBalance());
        return ResponseEntity.ok(response);
    }
	
	/**
     * Returns the current wallet balance of the authenticated user.
     *
     * Logging:
     * - Uses DEBUG level to avoid excessive production logs
     *
     * @param authentication Authenticated user context
     * 
     * @return Wallet balance and last updated timestamp
     * 
     * @throws UserNotFoundException if user does not exist
     * @throws WalletNotFoundException if wallet is not found
     */
	@GetMapping("/me")
	public ResponseEntity<WalletResponseDto> viewWallet(
	        Authentication authentication
	) throws UserNotFoundException, WalletNotFoundException {
		log.debug("Fetching wallet details for user: {}", authentication.getName());
	    return ResponseEntity.ok(walletService.viewWallet(authentication));
	}
	
	/**
	 * Performs a peer-to-peer wallet transfer using receiver's mobile number.
	 *
	 * @param key idempotency key for retry-safe transfer
	 * @param authentication authenticated sender context
	 * @param dto transfer request payload
	 *
	 * @return transaction details of the transfer
	 *
	 * @throws SenderWalletNotFoundException if sender wallet is missing
	 * @throws ReceiverNotFoundException if receiver user is not found
	 * @throws ReceiverWalletNotFoundException if receiver wallet is missing
	 * @throws SelfTransferNotAllowedException if sender tries to transfer to self
	 * @throws MpinNotSetException if MPIN is not set
	 * @throws InvalidMpinException if MPIN verification fails
	 * @throws InsufficientBalanceException if wallet balance is insufficient
	 * @throws KycNotApprovedException if sender's KYC is not approved
	 * @throws ResourceNotFoundException if required resources are missing
	 */
	@PostMapping("/transfer")
	public ResponseEntity<WalletTransferResponseDto> transfer(
				@RequestHeader("Idempotency-Key") String key,
				Authentication authentication,
				@Valid @RequestBody WalletTransferRequestDto dto
			) throws SenderWalletNotFoundException, ReceiverNotFoundException, SelfTransferNotAllowedException, MpinNotSetException, UserNotFoundException, InvalidMpinException, ReceiverWalletNotFoundException, InsufficientBalanceException, KycNotApprovedException, ResourceNotFoundException {
		log.info("REST request for P2P transfer: From={}, To={}, Amount={}, Key={}", 
				authentication.getName(), dto.getReceiverMobile(), dto.getAmount(), key);
		
		WalletTransferResponseDto response = walletService.transferWalletToWallet(authentication, dto);
		
		log.info("Transfer successful. Transaction ID: {}, Sender: {}, Receiver: {}", 
				response.getTxId(), authentication.getName(), dto.getReceiverMobile());
		
		return ResponseEntity.ok(response);
	}
	
	/**
	 * Provides spending insights for the authenticated user.
	 *
	 * @param auth authenticated user context
	 * @return spending insight for current and previous month
	 *
	 * @throws UserNotFoundException if user does not exist
	 * @throws ResourceNotFoundException if wallet or transaction data is missing
	 */
	@GetMapping("/spending-insight")
	public ResponseEntity<SpendingInsightDto> getSpendingInsight(Authentication auth) throws UserNotFoundException, ResourceNotFoundException {
		log.debug("Fetching spending insights for user: {}", auth.getName());
		return ResponseEntity.ok(walletService.getSpendingInsight(auth));
	}
}
