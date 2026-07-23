package com.example.demo.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.HighValueMerchantSummaryDto;
import com.example.demo.dto.MerchantHighValueTxnRowDto;
import com.example.demo.dto.PageResponse;
import com.example.demo.service.AdminMerchantDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/merchant")
public class AdminMerchantDashboardController {
	private final AdminMerchantDashboardService adminMerchantDashboardService;
	
	@GetMapping("/high-value/summary")
	public ResponseEntity<PageResponse<HighValueMerchantSummaryDto>> summary(
	        @RequestParam(defaultValue = "50000") BigDecimal minAmount,
	        @RequestParam(required = false) String q,
	        @RequestParam(defaultValue = "20") int limit,
	        @RequestParam(defaultValue = "0") int offset
	) {
	    PageResponse<HighValueMerchantSummaryDto> response =
	            adminMerchantDashboardService.getHighValueMerchantSummary(minAmount, q, limit, offset);
	    return ResponseEntity.ok(response);
	}

	
	@GetMapping("/{merchantId}/high-value/transactions")
    public ResponseEntity<List<MerchantHighValueTxnRowDto>> transactions(
            @PathVariable Long merchantId,
            @RequestParam(defaultValue = "50000") BigDecimal minAmount,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
		List<MerchantHighValueTxnRowDto> response = adminMerchantDashboardService.getMerchantHighValueTransactions(merchantId, minAmount, limit, offset);
		return ResponseEntity.ok(response);
	}
	
}
