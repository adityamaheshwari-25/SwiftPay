package com.example.demo.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder
public class CollectionStatsResponse {
	private BigDecimal today;     // Total collected today
    private BigDecimal monthly;   // Total collected this month
    private BigDecimal pending;   // Collected but not yet settled to bank
    private double dailyGrowthRate;   // % change from yesterday
    private double monthlyGrowthRate; // % change from last month
}
