package com.example.demo.repository.impl;

import java.math.BigDecimal;
import java.util.List;


import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.example.demo.dto.HighValueMerchantSummaryDto;
import com.example.demo.repository.AdminMerchantSummaryRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AdminMerchantSummaryRepositoryImpl implements AdminMerchantSummaryRepository{
	
	private final JdbcTemplate jdbcTemplate;
	
	// A RowMapper<T> converts one row of SQL result into one Java object of type T.
	/*
	 * Why static final?

		static: only one instance shared across all repository instances
		
		final: constant reference
		
		Efficient + thread-safe (because it’s stateless).
	 * */
	private static final RowMapper<HighValueMerchantSummaryDto> MAPPER = 
			(rs, rowNum) -> new HighValueMerchantSummaryDto(
						rs.getLong("merchantId"),
						rs.getString("merchantCode"),
			            rs.getString("businessName"),
			            rs.getString("category"),
			            rs.getLong("highValueTxnCount"),
			            rs.getBigDecimal("totalHighValueAmount"),
			            rs.getLong("distinctPayers")
					);
	
			
	/*
	 * jdbcTemplate.query(...)

	Executes a SQL query that returns multiple rows.
	
	It will:
	
	prepare statement
	
	bind parameters in order
	
	execute
	
	loop over ResultSet
	
	call MAPPER for each row
	
	return a List<HighValueMerchantSummaryDto>
	*/
	@Override
	public List<HighValueMerchantSummaryDto> fetchSummary(BigDecimal minAmount, String q, int limit, int offset) {
		  // We pass q as-is; the stored procedure will normalize NULL/blank safely.
		  return jdbcTemplate.query(
		         "CALL sp_admin_high_value_merchants_summary(?,?,?,?)",
		         MAPPER,
		         minAmount, q, limit, offset
		  );
	}

	/*
	 * 
	 * queryForObject(...)

		Used when you expect a single value back (exactly 1 row / 1 column).
		
		You provide:
		
		SQL
		
		required return type: Long.class
		
		parameters: minAmount
	 * 
	 * */
	@Override
	public Long fetchTotalMerchants(BigDecimal minAmount, String q) {
        return jdbcTemplate.queryForObject(
            "CALL sp_admin_high_value_merchants_count(?, ?)",
            Long.class,
            minAmount, q
        );
    }
	
}
