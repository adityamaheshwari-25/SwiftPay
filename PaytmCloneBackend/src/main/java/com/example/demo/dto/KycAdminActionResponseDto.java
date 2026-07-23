package com.example.demo.dto;

import com.example.demo.entity.enums.KycStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KycAdminActionResponseDto {
	private String message;
	private KycStatus status;
}
