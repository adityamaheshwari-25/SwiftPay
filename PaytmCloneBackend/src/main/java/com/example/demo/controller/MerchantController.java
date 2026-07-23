package com.example.demo.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CollectionStatsResponse;
import com.example.demo.dto.MerchantDashboardResponseDto;
import com.example.demo.dto.MerchantSettlementTransactionDto;
import com.example.demo.dto.MerchantTransactionResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.exception.LessAmountException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.MerchantService;
import com.example.demo.service.MerchantSettlementService;
import com.example.demo.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MerchantController
 * ------------------
 * Exposes merchant-specific APIs for dashboard, transactions,
 * settlement history, and fund settlements.
 *
 * Responsibilities:
 * - Fetch merchant transaction history
 * - Provide dashboard analytics & collection statistics
 * - Handle settlement workflows (instant & historical)
 *
 * Security:
 * - All endpoints are restricted to MERCHANT role
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchant")
@PreAuthorize("hasRole('MERCHANT')")
@RequiredArgsConstructor
public class MerchantController {
	private final TransactionService transactionService;
	private final MerchantService merchantService;
	private final MerchantSettlementService merchantSettlementService;
	private final CurrentUserService currentUserService;

    /**
     * Retrieves paginated transaction history for the authenticated merchant.
     *
     * Defaults:
     * - Page size: 15
     * - Sort: createdAt DESC
     *
     * @param authentication authenticated merchant context
     * @param pageable pagination and sorting configuration
     * @return paginated list of merchant transactions
     * @throws UserNotFoundException if merchant does not exist
     */
    @GetMapping("/dashboard/transactions")
    public ResponseEntity<Page<MerchantTransactionResponseDto>> getMerchantTransactions(
            Authentication authentication,
            @PageableDefault(size = 15, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) throws UserNotFoundException {
    	log.debug("Fetching all the transactions for the merchant [{}]", authentication.getName());
        return ResponseEntity.ok(transactionService.getMerchantTransactions(authentication, pageable));
    }
    
    /**
     * Fetches collection statistics for the authenticated merchant.
     *
     * @param auth authenticated merchant context
     * @return collection statistics summary
     * @throws UserNotFoundException if merchant does not exist
     * @throws ResourceNotFoundException if dependent resources are missing
     */
    @GetMapping("/stats")
    public ResponseEntity<CollectionStatsResponse> getStats(Authentication auth) throws UserNotFoundException, ResourceNotFoundException {
    	log.debug("Fetching stats for the merchant [{}]", auth.getName());
    	return ResponseEntity.ok(merchantService.getCollectionStats(auth));
    }
    
    /**
     * Retrieves paginated settlement history for the authenticated merchant.
     *
     * @param auth authenticated merchant context
     * @param pageable pagination configuration
     * @return paginated settlement transaction history
     * @throws UserNotFoundException if merchant does not exist
     * @throws ResourceNotFoundException if settlement data is missing
     */
    @GetMapping("/settlements")
    public ResponseEntity<Page<MerchantSettlementTransactionDto>> getSettlementHistory(
            Authentication auth, 
            Pageable pageable) throws UserNotFoundException, ResourceNotFoundException {
    	AppUser user = currentUserService.getCurrentUser(auth);
    	log.debug("Fetching all the settlements for the merchant [{}]", auth.getName());
        return ResponseEntity.ok(merchantSettlementService.getSettlementHistory(user, pageable));
    }
    
    /**
     * Triggers an instant settlement for the authenticated merchant.
     *
     * @param auth authenticated merchant context
     * @return confirmation message on successful settlement
     * @throws UserNotFoundException if merchant does not exist
     * @throws LessAmountException if settlement amount is below minimum
     * @throws ResourceNotFoundException if required resources are missing
     */
    @PostMapping("/settlements/instant")
    public ResponseEntity<String> instantSettlement(Authentication auth) throws UserNotFoundException, LessAmountException, ResourceNotFoundException {
    	log.info("Merchant [{}] started with the instant settlement", auth.getName());
    	AppUser user = currentUserService.getCurrentUser(auth);
    	merchantSettlementService.processInstantSettlement(user);
    	log.info("Instant settlement completed for merchant [{}]", auth.getName());
    	return ResponseEntity.ok("Funds move to the bank successfully!");
    }
    
    /**
     * Fetches dashboard data for the authenticated merchant.
     *
     * @param auth authenticated merchant context
     * @return merchant dashboard response
     * @throws UserNotFoundException if merchant does not exist
     * @throws ResourceNotFoundException if dependent resources are missing
     */
    @GetMapping("/dashboard")
    public ResponseEntity<MerchantDashboardResponseDto> getMerchantDashboard(Authentication auth) throws UserNotFoundException, ResourceNotFoundException {
    	log.debug("Fetching all the dashboard related data for the merchant [{}]", auth.getName());
    	return ResponseEntity.ok(merchantService.getMerchantDashboard(auth));
    }
  
}
