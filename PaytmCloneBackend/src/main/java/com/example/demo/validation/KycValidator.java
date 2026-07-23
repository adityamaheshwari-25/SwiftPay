package com.example.demo.validation;

import org.springframework.stereotype.Component;

import com.example.demo.entity.AppUser;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.KycNotApprovedException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KycValidator {
	public void validateKycStatus(AppUser user) {
        // Assuming your AppUser has a kycStatus or isKycApproved field
        if (!user.isKycVerified()) { 
            log.warn("Settlement blocked: Merchant {} has not completed KYC", user.getEmail());
            throw new KycNotApprovedException(ErrorMessage.KYC_NOT_APPROVED);
        }
    }
}
