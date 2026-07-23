package com.example.demo.service;

import org.springframework.security.core.Authentication;

import com.example.demo.dto.KycStatusResponseDto;
import com.example.demo.dto.SecurityStatusResponseDto;
import com.example.demo.dto.UserDashboardResponseDto;
import com.example.demo.dto.UserLookupResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;

public interface AppUserService {
	UserLookupResponseDto lookupByMobile(String mobile) throws UserNotFoundException;
	KycStatusResponseDto getKycStatus(Authentication authentication) throws UserNotFoundException, ResourceNotFoundException;
	SecurityStatusResponseDto getSecurityStatus(Authentication auth) throws UserNotFoundException;
	UserResponseDto getMyProfile(Authentication authentication) throws UserNotFoundException;
	UserDashboardResponseDto getUserDashboard(Authentication auth) throws UserNotFoundException, WalletNotFoundException, ResourceNotFoundException;
}
