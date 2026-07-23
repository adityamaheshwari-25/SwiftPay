package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.SplitParticipantState;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SplitParticipantDto {
	private Long userId;
	private String name;
	private String mobile;
	private BigDecimal shareAmount;
	private SplitParticipantState state;
	private String paidTxId;
	private LocalDateTime paidAt;
}
