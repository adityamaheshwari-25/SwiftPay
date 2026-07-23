package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.KycNotApprovedException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.KycDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KycValidationService {
	private final KycDocumentRepository kycDocumentRepository;
	
	public void ensureKycApproved(AppUser user) throws ResourceNotFoundException, KycNotApprovedException {
		KycDocument kyc = kycDocumentRepository
				.findByUser(user)
				.orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.KYC_NOT_FOUND));
		
		if (kyc.getStatus() != KycStatus.APPROVED) {
			throw new KycNotApprovedException(ErrorMessage.KYC_NOT_APPROVED);
		}
	}
}
