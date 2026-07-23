package com.example.demo.service.impl;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.KycStatusResponseDto;
import com.example.demo.dto.SecurityStatusResponseDto;
import com.example.demo.dto.SpendingInsightDto;
import com.example.demo.dto.UserDashboardResponseDto;
import com.example.demo.dto.UserLookupResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.dto.UserTransactionResponseDto;
import com.example.demo.dto.WalletResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.entity.enums.Role;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.AppUserService;
import com.example.demo.service.BankAccountService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserServiceImpl implements AppUserService{
	
	private final AppUserRepository appUserRepository;
	private final CurrentUserService currentUserService;
	private final KycDocumentRepository kycDocumentRepository;
	private final WalletService walletService;
	private final BankAccountService bankAccountService;
	private final TransactionService transactionService;
	
    /**
     * Looks up a user by mobile number.
     * <p>
     * Used internally for P2P transfers, user discovery, and validation flows.
     * For merchants, business name and merchant code are returned instead of personal name.
     *
     * @param mobile mobile number of the user to look up
     * @return basic user details including role, KYC status, and merchant info (if applicable)
     * @throws UserNotFoundException if no user exists with the given mobile number
     */
	@Override
	public UserLookupResponseDto lookupByMobile(String mobile) throws UserNotFoundException{
		
		log.debug("Internal lookup: searching for the user with mobile: {}", mobile);
		
		AppUser user = appUserRepository
						.findByMobile(mobile)
						.orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
		
		log.info("User lookup successful: ID={}, Role={}", user.getId(), user.getRole());
		
		String displayName = user.getName();
		String merchantCode = null;
		
		if (user.getRole() == Role.MERCHANT && user.getMerchant() != null) {
			displayName = user.getMerchant().getBusinessName();
			merchantCode = user.getMerchant().getMerchantCode();
		}
		
		return new UserLookupResponseDto(
					user.getId(),
					displayName,
					user.getMobile(),
					user.getRole(),
					user.isActive(),
					user.isKycVerified(),
					merchantCode
				);
	}

    /**
     * Retrieves the current KYC status of the authenticated user.
     * <p>
     * If no KYC document exists, status is returned as {@code NOT_APPLIED}.
     *
     * @param authentication authenticated user context
     * @return current KYC status and rejection reason (if any)
     * @throws UserNotFoundException if the authenticated user does not exist
     * @throws ResourceNotFoundException if user context resolution fails
     */
	@Override
	public KycStatusResponseDto getKycStatus(Authentication authentication) throws UserNotFoundException, ResourceNotFoundException {
		AppUser user = currentUserService.getCurrentUser(authentication);
		
		log.debug("Checking KYC status for user: {}", user.getEmail());

	    return kycDocumentRepository.findByUser(user)
	            .map(kyc -> 
	            {	
	            	log.debug("KYC record found. Status: {}", kyc.getStatus());
	            		return new KycStatusResponseDto(
		                    kyc.getStatus(),
		                    kyc.getRejectionReason()
	            		);
	            })
	            .orElseGet(() -> {
	            	log.debug("No KYC record found for user: {}", user.getEmail());
	                return new KycStatusResponseDto(KycStatus.NOT_APPLIED, null);
				});
	}
	
    /**
     * Retrieves the security posture of the authenticated user.
     * <p>
     * Includes MPIN status and KYC state to drive conditional flows
     * such as wallet usage, withdrawals, and transfers.
     *
     * @param authentication authenticated user context
     * @return aggregated security status (MPIN + KYC)
     * @throws UserNotFoundException if the authenticated user does not exist
     */
	@Override
	public SecurityStatusResponseDto getSecurityStatus(Authentication authentication) throws UserNotFoundException {
	    // 1. Get User
	    AppUser user = appUserRepository.findByEmail(authentication.getName())
	            .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));

	    // 2. Get KYC Info (from KycDocument table)
	    // If no document exists, status is NOT_APPLIED
	    KycDocument kyc = kycDocumentRepository.findByUser(user).orElse(null);
	    
	    KycStatus status = (kyc == null) ? KycStatus.NOT_APPLIED : kyc.getStatus();
	    String reason = (kyc != null) ? kyc.getRejectionReason() : null;

	    log.debug("Security status check for {}: MPIN_SET={}, KYC_STATUS={}", user.getEmail(), user.isMpinSet(), status);;
	    
	    return SecurityStatusResponseDto.builder()
	            .mpinSet(user.isMpinSet())
	            .kycStatus(status)
	            .rejectionReason(reason)
	            .build();
	}

    /**
     * Fetches the profile information of the authenticated user.
     *
     * @param authentication authenticated user context
     * @return user profile details
     * @throws UserNotFoundException if the authenticated user does not exist
     */
	@Override
	public UserResponseDto getMyProfile(Authentication authentication) throws UserNotFoundException {
		// authentication.getName() returns the email/username used to login
        AppUser user = appUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
        
        log.debug("Profile retrieved for user ID: {}", user.getId());
        
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName()) // adjust based on your entity field name
                .email(user.getEmail())
                .phoneNumber(user.getMobile())
                .role(user.getRole().name())
                .build();
	}

    /**
     * Builds the complete user dashboard.
     * <p>
     * Aggregates profile, wallet, linked bank accounts, recent transactions,
     * security status, and spending insights into a single response.
     *
     * @param auth authenticated user context
     * @return fully composed user dashboard response
     * @throws UserNotFoundException if the authenticated user does not exist
     * @throws WalletNotFoundException if the user's wallet does not exist
     * @throws ResourceNotFoundException if any dependent resource is missing
     */
	@Override
	public UserDashboardResponseDto getUserDashboard(Authentication auth) throws UserNotFoundException, WalletNotFoundException, ResourceNotFoundException {
		String email = auth.getName();
		log.info("Building dashboard for user: {}", email);
		
		long startTime = System.currentTimeMillis();
		
		UserResponseDto profile = getMyProfile(auth);
		WalletResponseDto wallet = walletService.viewWallet(auth);
		List<BankAccountResponseDto> bankAccounts = bankAccountService.getMyBankAccounts(email);
		
		Pageable pageable = PageRequest.of(0, 10);
		Page<UserTransactionResponseDto> transactions = transactionService.getMyTransactions(auth, pageable);
		
		SecurityStatusResponseDto security = getSecurityStatus(auth);
		SpendingInsightDto spending = walletService.getSpendingInsight(auth);
		
		long duration = System.currentTimeMillis() - startTime;
		log.info("Dashboard construction completed in {}ms for user: {}", duration, email);
		
		
		return UserDashboardResponseDto.builder()
				.profile(profile)
				.wallet(wallet)
				.bankAccounts(bankAccounts)
				.transactions(transactions)
				.security(security)
				.spending(spending)
				.build();
	}
	
	
	
	
	
	
	
}
