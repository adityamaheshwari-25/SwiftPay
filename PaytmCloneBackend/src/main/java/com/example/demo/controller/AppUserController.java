package com.example.demo.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.KycDocumentViewDto;
import com.example.demo.dto.KycFileDataDto;
import com.example.demo.dto.KycStatusResponseDto;
import com.example.demo.dto.SecurityStatusResponseDto;
import com.example.demo.dto.UserDashboardResponseDto;
import com.example.demo.dto.UserLookupResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;
import com.example.demo.service.AppUserService;
import com.example.demo.service.KycDocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 *  Exposes user-centric APIs.
 * */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AppUserController {
 
	private final AppUserService appUserService;
	private final KycDocumentService kycDocumentService;
	
	/**
     * Looks up a user by mobile number for P2P workflows.

     *
     * @param mobile registered mobile number
     * @return basic user lookup information
     * @throws UserNotFoundException if no user exists with the given mobile
     */
	@GetMapping("/lookup")
	public ResponseEntity<UserLookupResponseDto> lookupUser(@RequestParam String mobile) throws UserNotFoundException {
		log.debug("Lookup user for mobile no. {}", mobile);
		return ResponseEntity.ok(appUserService.lookupByMobile(mobile));
	}
	
    /**
     * Fetches the authenticated user's KYC details.
     *
     * @param authentication authenticated user context
     * @return KYC document metadata (without the actual file)
     * @throws UserNotFoundException if user is invalid
     * @throws ResourceNotFoundException if KYC record does not exist
     */
	@GetMapping("/kyc/me")
	public ResponseEntity<KycDocumentViewDto> viewMyKyc(Authentication authentication) throws UserNotFoundException, ResourceNotFoundException {
		log.debug("Getting kyc for user [{}]", authentication.getName());
		return ResponseEntity.ok(kycDocumentService.viewMyKyc(authentication));
	}
	
    /**
     * Streams the authenticated user's KYC document file.
     *
     * @param authentication authenticated user context
     * @return streamed KYC document as a Resource
     * @throws IOException if file cannot be read
     * @throws ResourceNotFoundException if file metadata is missing
     * @throws UserNotFoundException if user is invalid
     */
	@GetMapping("/kyc/me/file")
	@PreAuthorize("hasRole('USER') or hasRole('MERCHANT')")
	public ResponseEntity<Resource> viewMyKycFile(Authentication authentication) throws IOException, ResourceNotFoundException, UserNotFoundException {
		
		log.debug("Fetching kyc file for the user [{}]", authentication.getName());
		KycFileDataDto fileData = kycDocumentService.getMyKycFile(authentication);
		
		Path path = Paths.get(fileData.getFilePath());
		Resource resource = (Resource) new UrlResource(path.toUri());
		
		return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(fileData.getContentType()))
					.header(HttpHeaders.CONTENT_DISPOSITION,
							 "inline; filename=\"" + fileData.getFileName() + "\"")
		            .body(resource);
	}
	
    /**
     * Returns the current KYC verification status of the user.
     *
     * @param authentication authenticated user context
     * @return KYC status response (PENDING / VERIFIED / REJECTED)
     * @throws UserNotFoundException if user is invalid
     * @throws ResourceNotFoundException if KYC record is missing
     */
	@GetMapping("/kyc/me/status")
	public ResponseEntity<KycStatusResponseDto> viewMyKycStatus(Authentication authentication) throws UserNotFoundException, ResourceNotFoundException {
		log.debug("Fetching kyc status for user {}", authentication.getName());
		return ResponseEntity.ok(appUserService.getKycStatus(authentication));
	}
	
    /**
     * Fetches the user's security configuration status.
     *
     * Includes checks such as:
     * - PIN setup
     * - Kyc Status
     *
     * @param auth authenticated user context
     * @return security status summary
     * @throws UserNotFoundException if user is invalid
     */
	@GetMapping("/security-status")
	public ResponseEntity<SecurityStatusResponseDto> getSecurityStatus(Authentication auth) throws UserNotFoundException {
	    log.debug("Fetching security status for the user {}", auth.getName());
		return ResponseEntity.ok(appUserService.getSecurityStatus(auth));
	}
	
    /**
     * Retrieves the authenticated user's profile information.
     *
     * @param authentication authenticated user context
     * @return user profile details
     * @throws UserNotFoundException if user is invalid
     */
	@GetMapping("/profile")
    public ResponseEntity<UserResponseDto> getMyProfile(Authentication authentication) throws UserNotFoundException {
        log.debug("Fetching user [{}] profile", authentication.getName());
		return ResponseEntity.ok(appUserService.getMyProfile(authentication));
    }
	
    /**
     * Fetches aggregated dashboard data for the user.
     *
     * Dashboard typically includes:
     * - Wallet balances
     * - Recent activity
     * - KYC & security indicators
     *
     * @param auth authenticated user context
     * @return user dashboard response
     * @throws UserNotFoundException if user is invalid
     * @throws WalletNotFoundException if wallet does not exist
     * @throws ResourceNotFoundException if dependent resources are missing
     */
	@GetMapping("/dashboard")
	public ResponseEntity<UserDashboardResponseDto> getDashboard(Authentication auth) throws UserNotFoundException, WalletNotFoundException, ResourceNotFoundException {
		log.debug("Fetching user dashboard for: {}", auth.getName());
		return ResponseEntity.ok(appUserService.getUserDashboard(auth));
	}
}
