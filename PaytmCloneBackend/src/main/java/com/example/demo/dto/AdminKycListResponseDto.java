package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.enums.KycStatus;
import com.example.demo.entity.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminKycListResponseDto {
	private Long userId;
	private String name;
	private String mobile;
	private Role role;
	private String documentType;
	private String fileName;
	private KycStatus status;
	private LocalDateTime submittedAt;
	private String rejectionReason;
}
