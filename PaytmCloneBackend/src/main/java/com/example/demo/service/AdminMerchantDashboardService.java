package com.example.demo.service;

import java.math.BigDecimal;
import java.util.List;

import com.example.demo.dto.HighValueMerchantSummaryDto;
import com.example.demo.dto.MerchantHighValueTxnRowDto;
import com.example.demo.dto.PageResponse;

public interface AdminMerchantDashboardService {
	PageResponse<HighValueMerchantSummaryDto> getHighValueMerchantSummary(BigDecimal minAmount, String q, int limit, int offset);
	List<MerchantHighValueTxnRowDto> getMerchantHighValueTransactions(Long merchantId, BigDecimal minAmount, int limit, int offset);
}
