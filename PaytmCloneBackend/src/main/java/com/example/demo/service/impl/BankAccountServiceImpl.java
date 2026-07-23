package com.example.demo.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.CreateBankAccountDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedAccessException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.BankAccountMapper;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.BankAccountRepository;
import com.example.demo.service.BankAccountService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link BankAccountService} that manages
 * user-linked bank accounts.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Linking new bank accounts</li>
 *   <li>Managing primary bank account selection</li>
 *   <li>Fetching user-owned bank accounts</li>
 * </ul>
 * </p>
 *
 * <p>
 * This service enforces <b>ownership, consistency, and business rules</b>,
 * while controllers handle authentication and request validation.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService{

	private final BankAccountRepository bankAccountRepository;
	private final BankAccountMapper bankAccountMapper;
	private final AppUserRepository appUserRepository;
	
    /**
     * Links a new bank account to a user.
     *
     * <p>
     * Business rules:
     * <ul>
     *   <li>User must exist</li>
     *   <li>If this is the user's first active bank account,
     *       it is automatically marked as primary</li>
     *   <li>Account defaults (balance, verified flags) are handled
     *       via entity lifecycle hooks</li>
     * </ul>
     * </p>
     *
     * @param userEmail authenticated user's email
     * @param dto bank account creation payload
     * @return {@link BankAccountResponseDto} of the created bank account
     * @throws UserNotFoundException if the user does not exist
     */
	@Override
	@Transactional
	public BankAccountResponseDto createBankAccount(String userEmail, CreateBankAccountDto dto) throws UserNotFoundException {
		
		log.info("Attempting to link new bank account for user: {}. Bank: {}", userEmail, dto.getBankName());
		
		AppUser user = appUserRepository.findByEmail(userEmail)
					.orElseThrow(() -> {
						log.error("Bank Account creation failed: User {} not found", userEmail);
						return new UserNotFoundException(ErrorMessage.USER_NOT_FOUND);
					});
		
		// Logic: If no active account exists, this first one MUST be primary
        boolean hasExistingAccount = bankAccountRepository.existsByUserAndActiveTrue(user);
		
		BankAccount bankAccount = bankAccountMapper.toEntity(dto, user);
		bankAccount.setPrimary(!hasExistingAccount);
		
		// Persist (balance + verified set via @PrePersist)
		BankAccount savedAccount = bankAccountRepository.save(bankAccount);
		
		log.info("Bank account successfully linked. ID: {}, Primary: {}, User: {}", 
				savedAccount.getId(), savedAccount.isPrimary(), userEmail);
		
		return bankAccountMapper.toResponseDto(savedAccount);
		
	}
	
    /**
     * Sets a specific bank account as the user's primary account.
     *
     * <p>
     * Business rules:
     * <ul>
     *   <li>User must exist</li>
     *   <li>Account must exist</li>
     *   <li>Account must belong to the requesting user</li>
     *   <li>Only one primary account is allowed at any time</li>
     * </ul>
     * </p>
     *
     * <p>
     * This operation is transactional to guarantee atomicity:
     * either the primary account is updated fully or not at all.
     * </p>
     *
     * @param userEmail authenticated user's email
     * @param accountId ID of the bank account to mark as primary
     * @throws UserNotFoundException if the user does not exist
     * @throws ResourceNotFoundException if the bank account does not exist
     * @throws UnauthorizedAccessException if the account does not belong to the user
     */
	@Transactional
	public void setPrimaryAccount(String userEmail, Long accountId) throws UserNotFoundException, ResourceNotFoundException, UnauthorizedAccessException {
		log.info("Request to change primary bank account to ID: {} for user: {}", accountId, userEmail);
		
		AppUser user = appUserRepository.findByEmail(userEmail)
				.orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
		
		bankAccountRepository.markAllAsNonPrimary(user);
		
		BankAccount account = bankAccountRepository.findById(accountId)
				.orElseThrow(() -> {
					log.warn("Primary account updated failed: Account ID {} not found", accountId);
					return new ResourceNotFoundException(ErrorMessage.BANK_ACCOUNT_NOT_FOUND);
				});
		
		if (!account.getUser().getId().equals(user.getId())) {
			log.error("SECURITY ALERT: User {} attempted to set primary status on account ID {} owned by User ID {}", 
					userEmail, accountId, account.getUser().getId());
			throw new UnauthorizedAccessException(ErrorMessage.ACCOUNT_OWNERSHIP_MISMATCH);
		}
		
		account.setPrimary(true);
		bankAccountRepository.save(account);
		log.info("Account ID: {} is now the primary account for user: {}", accountId, userEmail);
	}
	
	
    /**
     * Retrieves all bank accounts linked to the authenticated user.
     *
     * @param userEmail authenticated user's email
     * @return list of {@link BankAccountResponseDto}
     * @throws UserNotFoundException if the user does not exist
     */
	@Override
	public List<BankAccountResponseDto> getMyBankAccounts(String userEmail) throws UserNotFoundException {
		log.debug("Fetching bank accounts for user: {}", userEmail);
		AppUser user = appUserRepository.findByEmail(userEmail)
				.orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
	
		return bankAccountRepository.findByUser(user)
								.stream()
								.map(bankAccountMapper::toResponseDto)
								.toList();
	}
	
}
