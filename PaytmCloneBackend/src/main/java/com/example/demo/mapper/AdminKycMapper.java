package com.example.demo.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.dto.AdminKycListResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;

@Component
public class AdminKycMapper {
	
	public AdminKycListResponseDto toAdminKycListDto(KycDocument kyc) {
		
		AppUser user = kyc.getUser();
		
		return new AdminKycListResponseDto(
					user.getId(),
					user.getName(),
					user.getMobile(),
					user.getRole(),
					kyc.getDocumentType(),
					kyc.getFileName(),
					kyc.getStatus(),
					kyc.getSubmittedAt(),
					kyc.getRejectionReason()
				);
	}
}
