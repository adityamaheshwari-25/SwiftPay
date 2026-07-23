package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data 
@Builder
public class SettlementResponse {
    private String status;            // PENDING, COMPLETED, PROCESSING
    private BigDecimal amount;
    private LocalDateTime nextSettlementDate;
}
