package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.CreateBankAccountDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedAccessException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.service.BankAccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/bank-accounts")
@RequiredArgsConstructor
public class BankAccountController {
	
	private final BankAccountService bankAccountService;
	
    /**
     * Links a new bank account to the authenticated user.
     *
     *
     * @param dto validated bank account creation payload
     * @param authentication authenticated user context
     * @return created bank account details
     * @throws UserNotFoundException if authenticated user does not exist
     */
	@PostMapping
	public ResponseEntity<BankAccountResponseDto> createBankAccont(
				@Valid @RequestBody CreateBankAccountDto dto,
				Authentication authentication
			) throws UserNotFoundException {
		log.info("Request to link new bank account for user: {}, Bank: {}", 
				authentication.getName(), dto.getBankName());
		
		BankAccountResponseDto response = bankAccountService.createBankAccount(authentication.getName(), dto);
		
		log.info("Successfully linked bank account ID: {} to user: {}", 
				response.getBankAccountId(), authentication.getName());
		
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}
	
    /**
     * Fetches all bank accounts linked to the authenticated user.
     *
     * @param authentication authenticated user context
     * @return list of linked bank accounts
     * @throws UserNotFoundException if authenticated user does not exist
     */
	@GetMapping
	public ResponseEntity<List<BankAccountResponseDto>> getMyBankAccounts(
			Authentication authentication
			) throws UserNotFoundException {
		log.debug("Fetching all linked bank accounts for user: {}", authentication.getName());
		
		List<BankAccountResponseDto> accounts = bankAccountService.getMyBankAccounts(authentication.getName());
		
		log.debug("Found {} bank accounts for user: {}", accounts.size(), authentication.getName());
		
		return ResponseEntity.ok(accounts);
	}
	
    /**
     * Sets a specific bank account as the user's primary account.
     *
     * Business rules enforced by service layer:
     * - Account must belong to the authenticated user
     * - Only one account can be primary at a time
     *
     * @param authentication authenticated user context
     * @param accountId ID of the bank account to be set as primary
     * @return empty response with HTTP 200 on success
     * @throws UserNotFoundException if user does not exist
     * @throws ResourceNotFoundException if bank account does not exist
     * @throws UnauthorizedAccessException if account does not belong to user
     */
	@PatchMapping("/{accountId}/set-primary")
    public ResponseEntity<Void> setPrimary(
            Authentication authentication, 
            @PathVariable Long accountId) throws UserNotFoundException, ResourceNotFoundException, UnauthorizedAccessException {
		log.info("User {} is setting bank account ID {} as primary", authentication.getName(), accountId);
		
        bankAccountService.setPrimaryAccount(authentication.getName(), accountId);
        
        log.info("Successfully updated primary account for user: {}", authentication.getName());
        
        return ResponseEntity.ok().build();
    }
	
	
}
