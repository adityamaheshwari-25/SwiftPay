package com.example.demo.repository;

import java.math.BigDecimal;
import java.util.List;

import com.example.demo.dto.MerchantHighValueTxnRowDto;

public interface AdminMerchantTxnRespository {
	List<MerchantHighValueTxnRowDto> fetchHighValueTxnsByMerchant(Long merchantId, BigDecimal minAmount, int limit, int offset); 
}
