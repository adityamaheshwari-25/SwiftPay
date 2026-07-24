package com.example.demo.service.impl;


import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AdminKycListResponseDto;
import com.example.demo.dto.KycAdminActionResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.KycNotPendingException;
import com.example.demo.exception.RejectWithoutReasonException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.AdminKycMapper;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.service.KycAdminService;
import com.example.demo.service.storage.DocumentStorageService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Administrative service responsible for manual KYC verification workflows.
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Approve or reject submitted KYC documents</li>
 *   <li>Retrieve pending KYC applications</li>
 *   <li>Stream KYC documents for manual review</li>
 * </ul>
 * </p>
 *
 * <p>
 * This service is intended to be accessed only by privileged
 * (ADMIN) endpoints and enforces strict KYC state transitions.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KycAdminServiceImpl implements KycAdminService{
	
	private final KycDocumentRepository kycDocumentRepository;
	private final AdminKycMapper adminKycMapper;
	private final DocumentStorageService documentStorageService;
	
    /**
     * Approves a user's pending KYC application.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Load KYC document by userId</li>
     *   <li>Validate that KYC is currently in {@link KycStatus#PENDING}</li>
     *   <li>Update KYC status to {@link KycStatus#APPROVED}</li>
     *   <li>Mark user as KYC verified</li>
     * </ol>
     *
     * @param userId the user whose KYC should be approved
     * @return response DTO describing the action taken
     * @throws ResourceNotFoundException if no KYC document exists for the user
     * @throws KycNotPendingException if KYC is not currently in PENDING state
     * 
     * @CacheEvict runs after successful method execution.

		If the method throws exception → cache is NOT cleared.
     */
	@Override
	@Transactional
	@CacheEvict(cacheNames = "pendingKyc", allEntries = true)
	public KycAdminActionResponseDto approveKyc(Long userId) throws ResourceNotFoundException, KycNotPendingException {
		log.info("Processing KYC approval for user id: {}", userId);
		
		KycDocument kyc = kycDocumentRepository
					.findByUserId(userId)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));
		
		if (kyc.getStatus() != KycStatus.PENDING) {
			throw new KycNotPendingException(ErrorMessage.KYC_IS_NOT_PENDING);
		}
		
		kyc.setStatus(KycStatus.APPROVED);
		kyc.setRejectionReason(null);
		kyc.setReviewedAt(LocalDateTime.now());
		
		AppUser user = kyc.getUser();
		user.setKycVerified(true);
		
		log.info("KYC status updated to APPROVED for User ID: {}. User account is now verified.", userId);
		
		return new KycAdminActionResponseDto(
					"KYC approved successfully",
					KycStatus.APPROVED
				);
	}

    /**
     * Rejects a user's pending KYC application.
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Load KYC document by userId</li>
     *   <li>Validate that KYC is currently {@link KycStatus#PENDING}</li>
     *   <li>Validate that a rejection reason is provided</li>
     *   <li>Update KYC status to {@link KycStatus#REJECTED}</li>
     *   <li>Mark user as NOT KYC verified</li>
     * </ol>
     *
     * @param userId the user whose KYC should be rejected
     * @param reason the reason for rejection (required)
     * @return response DTO describing the action taken
     * @throws ResourceNotFoundException if no KYC document exists for the user
     * @throws KycNotPendingException if KYC is not currently in PENDING state
     * @throws RejectWithoutReasonException if rejection reason is missing
     */
	@Override
	@Transactional
	@CacheEvict(cacheNames = "pendingKyc", allEntries = true)
	public KycAdminActionResponseDto rejectKyc(Long userId, String reason) throws ResourceNotFoundException, KycNotPendingException, RejectWithoutReasonException {
		log.warn("Processing KYC rejection for User ID: {}. Reason provided: {}", userId, reason);
		
		KycDocument kyc = kycDocumentRepository
				.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));
	
		if (kyc.getStatus() != KycStatus.PENDING) {
			throw new KycNotPendingException(ErrorMessage.KYC_IS_NOT_PENDING);
		}
		
		if (reason == null) {
			throw new RejectWithoutReasonException(ErrorMessage.REJECTING_WITHOUT_GIVING_REASON);
		}
		
		kyc.setStatus(KycStatus.REJECTED);
		kyc.setRejectionReason(reason);
		kyc.setReviewedAt(LocalDateTime.now());
		
//		Update user-level verification flag
		AppUser user = kyc.getUser();
		user.setKycVerified(false);
		
		log.info("KYC status updated to REJECTED for User ID: {}", userId);
		
		return new KycAdminActionResponseDto(
					"Kyc rejected successfully",
					KycStatus.REJECTED
				);
	}
	
    /**
     * Returns all KYC applications currently in {@link KycStatus#PENDING} state.
     *
     * <p>
     * Used by admin screens to review KYC submissions.
     * </p>
     *
     * @return list of pending KYC application DTOs
     */
	@Override
	@Cacheable(cacheNames = "pendingKyc")
	public List<AdminKycListResponseDto> getPendingKycList() {
		log.debug("Fetching all pending KYC applications from database");
		return kycDocumentRepository
				.findByStatus(KycStatus.PENDING)
				.stream()
				.map(adminKycMapper::toAdminKycListDto)
				.collect(Collectors.toList());
	}

    /**
     * Streams the KYC document for a given user as a {@link Resource}.
     *
     * <p>
     * The response is configured to display the document inline in the browser
     * (Content-Disposition: inline), making it convenient for admin review screens.
     * </p>
     *
     * @param userId the user whose KYC document should be retrieved
     * @return HTTP response with content type and inline disposition set
     * @throws ResourceNotFoundException if the user has no KYC record or file is missing
     */
	@Override
	@Transactional
	public ResponseEntity<Resource> viewKycByUserId(Long userId) throws ResourceNotFoundException {
		log.info("Admin is retrieving document for User ID: {}", userId);
		KycDocument kyc = kycDocumentRepository.findByUserId(userId)
					.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));
		
		return buildResponse(kyc);
	}
	
    /**
     * Builds a {@link ResponseEntity} that streams the user's KYC document as an HTTP response.
     *
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Loads {@link KycDocument} by {@link AppUser}</li>
     *   <li>Loads the document bytes from Azure Blob Storage</li>
     *   <li>Sets the Content-Type based on stored document metadata</li>
     *   <li>Uses Content-Disposition inline so browsers can render it directly</li>
     * </ul>
     * </p>
     *
     * @param user the user whose document should be streamed
     * @return HTTP response containing the file as a {@link Resource}
     * @throws ResourceNotFoundException if the KYC record or file cannot be found
     */
	private ResponseEntity<Resource> buildResponse(KycDocument kyc) throws ResourceNotFoundException {
		byte[] fileData = documentStorageService.load(kyc.getStorageKey())
				.filter(data -> data.length > 0)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));
		if (fileData.length == 0) {
			throw new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND);
		}

		Resource resource = new ByteArrayResource(fileData);

		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(kyc.getContentType()))
				.header(
						HttpHeaders.CONTENT_DISPOSITION,
	                    "inline; filename=\"" + kyc.getFileName() + "\""
				)
				.body(resource);
	}
	
}
