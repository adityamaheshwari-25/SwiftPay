package com.example.demo.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import com.example.demo.dto.AdminKycListResponseDto;
import com.example.demo.dto.KycAdminActionResponseDto;
import com.example.demo.exception.KycNotPendingException;
import com.example.demo.exception.RejectWithoutReasonException;
import com.example.demo.exception.ResourceNotFoundException;

public interface KycAdminService {
	KycAdminActionResponseDto approveKyc(Long userId) throws ResourceNotFoundException, KycNotPendingException;
	KycAdminActionResponseDto rejectKyc(Long userId, String reason) throws ResourceNotFoundException, KycNotPendingException, RejectWithoutReasonException;
	List<AdminKycListResponseDto> getPendingKycList();
	ResponseEntity<Resource> viewKycByUserId(Long userId) throws ResourceNotFoundException;
}
