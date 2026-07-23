package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entity.enums.KycStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class KycDocumentViewDto {
	private String documentType;
	private String fileName;
	private String contentType;
	private long fileSize;
	private KycStatus status;
	private String rejectionReason;
	private LocalDateTime submittedAt;
}
