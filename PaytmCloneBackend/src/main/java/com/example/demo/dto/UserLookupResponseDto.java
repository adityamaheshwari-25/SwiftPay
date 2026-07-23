package com.example.demo.dto;

import com.example.demo.entity.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserLookupResponseDto {
	private Long userId;
	// Display name for UI
    private String displayName;

    private String mobile;
    private Role role;

    private boolean active;
    private boolean kycVerified;

    // Only for merchants
    private String merchantCode;
}
