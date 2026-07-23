package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.SplitParticipantState;
import com.example.demo.entity.enums.SplitStatus;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SplitInvolvedListItemDto {
	private Long splitId;
    private String splitCode;

    private Long initiatorId;
    private String initiatorName;

    private BigDecimal totalAmount;
    private BigDecimal myShare;
    private SplitParticipantState myState;

    private SplitStatus status;
    private String note;
    private LocalDateTime createdAt;
}
