package com.example.demo.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AdminKycListResponseDto;
import com.example.demo.dto.KycAdminActionResponseDto;
import com.example.demo.dto.KycRejectRequestDto;
import com.example.demo.exception.KycNotPendingException;
import com.example.demo.exception.RejectWithoutReasonException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.service.KycAdminService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes administrative APIs for KYC verification workflows.
 *
 *
 * Security:
 * - Entire controller is restricted to ADMIN role
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/kyc")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class KycAdminController {
	
	private final KycAdminService kycAdminService;
	
    /**
     * Approves the KYC submission for a given user.
     *
     * Business rules enforced by service layer:
     * - KYC must exist
     * - KYC must be in PENDING state
     *
     * @param userId ID of the user whose KYC is being approved
     * @param authentication authenticated admin context
     * @return approval action result
     * @throws ResourceNotFoundException if user or KYC record does not exist
     * @throws KycNotPendingException if KYC is not in a pending state
     */
	@PostMapping("/{userId}/approve")
	public ResponseEntity<KycAdminActionResponseDto> approve(
				@PathVariable Long userId,
				Authentication authentication // Added to identify the admin
			) throws ResourceNotFoundException, KycNotPendingException {
		log.info("Admin [{}] is approving KYC for User ID: {}", authentication.getName(), userId);
		
		KycAdminActionResponseDto response = kycAdminService.approveKyc(userId);
		
		log.info("KYC successfully approved for User ID: {} by Admin: {}", userId, authentication.getName());
		
		return ResponseEntity.ok(response);
	}
	
    /**
     * Rejects the KYC submission for a given user.
     *

     * @param userId ID of the user whose KYC is being rejected
     * @param dto rejection request containing reason
     * @param authentication authenticated admin context
     * @return rejection action result
     * @throws ResourceNotFoundException if user or KYC record does not exist
     * @throws KycNotPendingException if KYC is not in pending state
     * @throws RejectWithoutReasonException if rejection reason is missing
     */
	@PostMapping("/{userId}/reject")
	public ResponseEntity<KycAdminActionResponseDto> reject(
			@PathVariable Long userId, 
			@Valid @RequestBody KycRejectRequestDto dto,
			Authentication authentication // Added to identify the admin
		) throws ResourceNotFoundException, KycNotPendingException, RejectWithoutReasonException {
		
		// using warn for rejections as they are significant negative outcomes for the user.
		log.warn("Admin [{}] is REJECTING KYC for User ID: {}. Reason: {}", authentication.getName(), userId, dto.getRejectionReason());
		
		KycAdminActionResponseDto response = kycAdminService.rejectKyc(userId, dto.getRejectionReason());
		
		log.info("KYC rejection processed for User ID: {}", userId);
		
		return ResponseEntity.ok(response);
	}
	
    /**
     * Retrieves all pending KYC applications.
     *
     * Typically used by admin dashboards and review queues.
     *
     * @return list of users with pending KYC submissions
     */
	@GetMapping("/pending")
	public ResponseEntity<List<AdminKycListResponseDto>> getPendingKyc() {
		log.debug("Admin requested list of pending KYC applications");
		
		List<AdminKycListResponseDto> list = kycAdminService.getPendingKycList();
		
		log.debug("Returning {} pending KYC applications", list.size());
		
		return ResponseEntity.ok(list);
	}
	
    /**
     * Streams the KYC document submitted by a specific user.
     *
     * Security:
     * - Restricted to ADMIN role
     * - Used strictly for manual KYC verification
     *
     * @param userId ID of the user whose KYC document is requested
     * @return streamed KYC document resource
     * @throws ResourceNotFoundException if document or user does not exist
     */
	@GetMapping("/{userId}/document")
	public ResponseEntity<Resource> viewUserKyc(@PathVariable Long userId) throws ResourceNotFoundException {
		log.info("Admin is viewing KYC document for User ID: {}", userId);
		
		return kycAdminService.viewKycByUserId(userId);
	}
}
