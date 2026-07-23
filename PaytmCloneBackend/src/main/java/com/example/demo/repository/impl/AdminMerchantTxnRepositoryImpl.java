package com.example.demo.repository.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.MerchantHighValueTxnRowDto;
import com.example.demo.repository.AdminMerchantTxnRespository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AdminMerchantTxnRepositoryImpl implements AdminMerchantTxnRespository{
	
	private final JdbcTemplate jdbcTemplate;

	private static final RowMapper<MerchantHighValueTxnRowDto> TXN_MAPPER = (ResultSet rs, int rowNum) -> {
	    Long payerUserId = rs.getObject("payerUserId") == null ? null : rs.getLong("payerUserId");
	    Timestamp ts = rs.getTimestamp("createdAt");
	
	    return new MerchantHighValueTxnRowDto(
	            rs.getLong("merchantId"),
	            rs.getString("merchantCode"),
	            rs.getString("businessName"),
	            rs.getString("category"),
	
	            payerUserId,
	            rs.getString("payerName"),
	            rs.getString("payerEmail"),
	            rs.getString("payerMobile"),
	
	            rs.getLong("transactionDbId"),
	            rs.getString("txId"),
	            rs.getString("referenceId"),
	            rs.getString("transactionType"),
	            rs.getString("paymentMode"),
	            rs.getBigDecimal("amount"),
	            rs.getString("status"),
	            ts == null ? null : ts.toLocalDateTime()
	    );
	};
	
	@Override
	public List<MerchantHighValueTxnRowDto> fetchHighValueTxnsByMerchant(Long merchantId, BigDecimal minAmount,
			int limit, int offset) {
		return jdbcTemplate.query(
	            "CALL sp_admin_merchant_high_value_txns_by_merchant(?,?,?,?)",
	            TXN_MAPPER,
	            merchantId, minAmount, limit, offset
	    );
	}
}
