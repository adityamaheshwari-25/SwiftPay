package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.List;

import com.example.demo.dto.HighValueMerchantSummaryDto;

public interface AdminMerchantSummaryRepository {
	List<HighValueMerchantSummaryDto> fetchSummary(BigDecimal minAmount, String q, int limit, int offset);
	Long fetchTotalMerchants(BigDecimal minAmount, String q);
}
