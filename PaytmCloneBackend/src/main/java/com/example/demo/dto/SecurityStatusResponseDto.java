package com.example.demo.dto;

import com.example.demo.entity.enums.KycStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityStatusResponseDto {
	private boolean mpinSet;
    private KycStatus kycStatus; // PENDING, APPROVED, REJECTED, NOT_APPLIED
    private String rejectionReason; // Only if REJECTED
}
