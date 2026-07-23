package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.SplitStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SplitCreatedListItemDto {
	private Long splitId;
    private String splitCode;
    private BigDecimal totalAmount;
    private SplitStatus status;
    private String note;
    private LocalDateTime createdAt;

    private int totalParticipants;
    private int paidParticipants;
}
