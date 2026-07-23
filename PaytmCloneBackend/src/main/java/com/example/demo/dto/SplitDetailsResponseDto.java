package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entity.enums.SplitStatus;
import com.example.demo.entity.enums.SplitType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SplitDetailsResponseDto {
	private Long splitId;
    private String splitCode;
    private Long initiatorUserId;
    private BigDecimal totalAmount;
    private SplitType splitType;
    private SplitStatus status;
    private String note;
    private LocalDateTime createdAt;
    private List<SplitParticipantDto> participants;
}
