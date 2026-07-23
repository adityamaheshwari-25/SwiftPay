package com.example.demo.service;

import org.springframework.security.core.Authentication;

import com.example.demo.dto.CollectionStatsResponse;
import com.example.demo.dto.MerchantDashboardResponseDto;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;

public interface MerchantService {
	CollectionStatsResponse getCollectionStats(Authentication auth) throws UserNotFoundException, ResourceNotFoundException;
	MerchantDashboardResponseDto getMerchantDashboard(Authentication auth) throws UserNotFoundException, ResourceNotFoundException, UserNotFoundException;
}
