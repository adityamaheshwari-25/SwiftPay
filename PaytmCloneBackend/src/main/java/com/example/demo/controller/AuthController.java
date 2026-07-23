package com.example.demo.controller;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.RegisterAppUserDto;
import com.example.demo.dto.RegisterMerchantDto;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


/**
 * Entry point for authentication and registration processes.
 * Handles both standard Users and Merchant onboarding workflows.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
	
	private final AuthService authService;
	
	/**
	 * Registers a new consumer-tier user.
	 * 
	 * @param req The validated registration details.
	 * @return AuthResponseDto containing the JWT and basic user info.
	 * @throws UserAlreadyExistsException if email is already registered
	 * */
	 @PostMapping("/users/register")
	 public ResponseEntity<AuthResponseDto> registerUser(@Valid @RequestBody RegisterAppUserDto req) throws UserAlreadyExistsException {
	     log.info("New user registration started for the email: {}", req.getEmail());
		 AuthResponseDto response = authService.registerUser(req);
		 log.info("User registration successful for: {}", req.getEmail());
	     return ResponseEntity.status(HttpStatus.CREATED).body(response);
	 }
	 
	 /**
	  * Registers a new merchant partner.
	  * 
	  * @param req validated merchant registration request
	  * @return AuthResponseDto containing JWT and merchant profile details
	  * @throws UserAlreadyExistsException if merchant email already exists
	  */
	 @PostMapping("/merchants/register")
	 public ResponseEntity<AuthResponseDto> registerMerchant(@Valid @RequestBody RegisterMerchantDto req) throws UserAlreadyExistsException {
		 log.info("New merchant registration started for the email: {}", req.getEmail());
		 AuthResponseDto response = authService.registerMerchant(req);
		 log.info("Merchant registration successful for: {}", req.getEmail());
		 return ResponseEntity.status(HttpStatus.CREATED).body(response);
	 }
	 
	 /**
	  * Authenticates an existing user.
	  * 
	  * @param req login request containing email and password.
	  * @return AuthResponseDto containing JWT and user details.
	  * @throws UserNotFoundException if user does not exist or credentials are invalid
	  * */
	 @PostMapping("/auth/login")
	 public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto req) throws UserNotFoundException {
	     log.info("Login attempt inititated for email: {}", req.getEmail());
		 AuthResponseDto response = authService.login(req);
		 log.info("User {} successfully logged in", req.getEmail());
		 return ResponseEntity.ok(response); // 200 OK
	 }
}
