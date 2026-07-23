package com.example.demo.service;

import java.io.OutputStream;
import java.math.BigDecimal;

public interface AdminMerchantReportService {
//	ByteArrayInputStream exportHighValueMerchantsSummaryXlsx(BigDecimal minAmount, String q);
	public void exportHighValueMerchantsSummaryXlsx(BigDecimal minAmount, String q, OutputStream outputStream);

//	ByteArrayInputStream exportMerchantHighValueTxnsXlsx(Long merchantId, BigDecimal minAmount);
	
	public void exportMerchantHighValueTxnsXlsx(Long merchantId, BigDecimal minAmount, OutputStream outputStream);
}
