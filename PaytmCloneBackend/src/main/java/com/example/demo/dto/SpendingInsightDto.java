package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SpendingInsightDto {
	private BigDecimal currentMonthSpent;
    private BigDecimal previousMonthSpent;
    private double percentageChange; // e.g., 12.5
    private boolean isIncrease;      // true if spent more than last month
}
