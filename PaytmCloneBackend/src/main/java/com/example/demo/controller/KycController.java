package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.KycDocumentUploadResponseDto;
import com.example.demo.exception.FileNotUploadedException;
import com.example.demo.exception.InvalidFileTypeException;
import com.example.demo.exception.KycAlreadySubmittedException;
import com.example.demo.exception.LargerFileSizeException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.service.KycDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles user-side KYC document submission.
 *
 * Responsibilities:
 * - Accepts KYC document uploads
 * - Associates uploaded document with authenticated user
 *
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/kyc")
@RequiredArgsConstructor
public class KycController {
	
	private final KycDocumentService kycDocumentService;
	
	/**
     * Uploads a KYC document for the authenticated user.
     *
     * @param file uploaded KYC document (e.g., ID proof)
     * @param authentication authenticated user context
     * @return response containing KYC submission details
     *
     * @throws UserNotFoundException if authenticated user does not exist
     * @throws FileNotUploadedException if file upload fails
     * @throws LargerFileSizeException if file exceeds allowed size
     * @throws InvalidFileTypeException if file format is not supported
     * @throws KycAlreadySubmittedException if KYC was already submitted
     * @throws IOException if file storage operation fails
     */
	@PostMapping("/upload")
	public ResponseEntity<KycDocumentUploadResponseDto> uploadKyc(
				@RequestParam("file") MultipartFile file,
				Authentication authentication
			) throws UserNotFoundException, FileNotUploadedException, LargerFileSizeException, InvalidFileTypeException, KycAlreadySubmittedException {
		log.info("User [{}] started uploading kyc document", authentication.getName());
		
		KycDocumentUploadResponseDto response = kycDocumentService.uploadKyc(authentication, file);
		
		log.info("User [{}] uploaded the kyc document", authentication.getName());
		
		return ResponseEntity.ok(response);
	}
}
