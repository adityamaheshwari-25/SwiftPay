package com.example.demo.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserTransactionResponseDto;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * TransactionController
 * ---------------------
 * Exposes APIs related to user transaction history.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
	
	private final TransactionService transactionService;
	
	/**
     * Fetches paginated transaction history for the authenticated user.
     *
     * Features:
     * - Pagination support
     * - Sorting by creation time (latest first)
     *
     * Default Pagination:
     * - Page size: 10
     * - Sort: createdAt (DESC)
     *
     * @param authentication authenticated user context
     * @param pageable pagination and sorting parameters
     * @return paginated list of user transactions
     * @throws UserNotFoundException if authenticated user does not exist
     */
	@GetMapping("/my")
	public ResponseEntity<Page<UserTransactionResponseDto>> getMyTransactions(
	        Authentication authentication,
	        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) throws UserNotFoundException {
	    
		log.debug("Fetching all the transactions for the user [{}]", authentication.getName());
	    Page<UserTransactionResponseDto> transactions = transactionService.getMyTransactions(authentication, pageable);
	    return ResponseEntity.ok(transactions);
	}
}
