package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.SetMpinRequestDto;
import com.example.demo.dto.VerifyMpinRequestDto;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.service.MpinService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Exposes APIs for managing user MPIN (Mobile PIN) operations.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class MpinController {
	
	private final MpinService mpinService;
	
    /**
     * Sets or updates the MPIN for the authenticated user.
     * 
     * @param authentication authenticated user context
     * @param dto MPIN setup request payload
     * @return HTTP 200 on successful MPIN setup
     * @throws UserNotFoundException if authenticated user does not exist
     */
	@PostMapping("/set-mpin")
	public ResponseEntity<Void> setMpin(
				Authentication authentication,
				@Valid @RequestBody SetMpinRequestDto dto
			) throws UserNotFoundException {
		log.info("User [{}] started setting mpin", authentication.getName());
		mpinService.setMpin(authentication, dto);
		log.info("Mpin set for the user [{}]", authentication.getName());
		return ResponseEntity.ok().build();
	}
	
    /**
     * Verifies the MPIN for the authenticated user.
     *
     * @param authentication authenticated user context
     * @param dto MPIN verification request payload
     * @return HTTP 200 if MPIN verification succeeds
     * @throws UserNotFoundException if authenticated user does not exist
     * @throws MpinNotSetException if MPIN has not been configured
     * @throws InvalidMpinException if provided MPIN is incorrect
     */
	@PostMapping("/verify-mpin")
	public ResponseEntity<Void> verifyMpin
			(Authentication authentication,
			@RequestBody VerifyMpinRequestDto dto	
	) throws UserNotFoundException, MpinNotSetException, InvalidMpinException {
		log.info("Mpin verification started for the user [{}]", authentication.getName());
		mpinService.verifyMpin(authentication, dto);
		log.info("Mpin verification completed for the user [{}]", authentication.getName());
		return ResponseEntity.ok().build();
	}
}
