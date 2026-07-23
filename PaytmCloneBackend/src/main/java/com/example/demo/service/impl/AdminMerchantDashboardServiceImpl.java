package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.example.demo.dto.HighValueMerchantSummaryDto;
import com.example.demo.dto.MerchantHighValueTxnRowDto;
import com.example.demo.dto.PageResponse;
import com.example.demo.repository.AdminMerchantSummaryRepository;
import com.example.demo.repository.AdminMerchantTxnRespository;
import com.example.demo.service.AdminMerchantDashboardService;

import lombok.RequiredArgsConstructor;

/**
 * Service implementation for Admin Merchant Dashboard use cases.
 *
 * <p>
 * This service acts as an orchestration layer between the controller
 * and the repository layer. It is responsible for:
 * <ul>
 *   <li>Fetching paginated high-value merchant summaries</li>
 *   <li>Fetching merchant-specific high-value transactions</li>
 *   <li>Combining data (items + total count) into a PageResponse where required</li>
 * </ul>
 *
 * <p>
 * Business logic is intentionally kept minimal here; this class mainly
 * coordinates repository calls and shapes the response expected by the API.
 */
@Service
@RequiredArgsConstructor
public class AdminMerchantDashboardServiceImpl implements AdminMerchantDashboardService{

	private final AdminMerchantSummaryRepository summaryRepository;
	private final AdminMerchantTxnRespository txnRespository;
	
	// cache the list(page)
	@Cacheable(cacheNames="hvMerchantSummary", key= "T(java.util.Objects).hash(#minAmount, #q, #limit, #offset)")
	public List<HighValueMerchantSummaryDto> cachedSummary(BigDecimal minAmount, String q, int 	limit, int offset) {
		return summaryRepository.fetchSummary(minAmount, q, limit, offset);
	}
	
	 // Cache the count (separately)
	 @Cacheable(cacheNames = "hvMerchantCount", key = "T(java.util.Objects).hash(#minAmount,#q)")
	 public Long cachedCount(BigDecimal minAmount, String q) {
	   return summaryRepository.fetchTotalMerchants(minAmount, q);
	 }
	
    /**
     * Fetches a paginated list of merchants having high-value transactions.
     *
     * <p>
     * This method performs two separate database calls:
     * <ol>
     *   <li>Fetch the current page of merchant summary records</li>
     *   <li>Fetch the total number of merchants matching the criteria</li>
     * </ol>
     *
     * <p>
     * The result is wrapped into a {@link PageResponse} object so that
     * the frontend can easily implement pagination.
     *
     * @param minAmount minimum transaction amount to qualify as "high value"
     * @param limit     maximum number of records to return
     * @param offset    number of records to skip (pagination offset)
     * @param q 		Name of the merchant business Name
     * @return a {@link PageResponse} containing merchant summaries and total count
     */
//	public PageResponse<HighValueMerchantSummaryDto> getHighValueMerchantSummary(
//	        BigDecimal minAmount, String q, int limit, int offset
//	) {
//	    List<HighValueMerchantSummaryDto> items = summaryRepository.fetchSummary(minAmount, q, limit, offset);
//	    long total = summaryRepository.fetchTotalMerchants(minAmount, q);
//	    return new PageResponse<>(items, total, limit, offset);
//	}
	  @Override
	  public PageResponse<HighValueMerchantSummaryDto> getHighValueMerchantSummary(BigDecimal minAmount, String q, int limit, int offset) {
	    List<HighValueMerchantSummaryDto> items = cachedSummary(minAmount, q, limit, offset);
	    long total = cachedCount(minAmount, q);
	    return new PageResponse<>(items, total, limit, offset);
	  }


    /**
     * Fetches high-value transactions for a specific merchant.
     *
     * <p>
     * Unlike the summary API, this method returns a simple list rather than
     * a {@link PageResponse}. Pagination is still supported via
     * {@code limit} and {@code offset}, but the total count is not required
     * for the current use case.
     *
     * @param merchantId the unique identifier of the merchant
     * @param minAmount  minimum transaction amount to qualify as "high value"
     * @param limit      maximum number of transactions to return
     * @param offset     number of transactions to skip (pagination offset)
     * @return list of {@link MerchantHighValueTxnRowDto} representing transactions
     */
	@Override
	public List<MerchantHighValueTxnRowDto> getMerchantHighValueTransactions(Long merchantId, BigDecimal minAmount,
			int limit, int offset) {
		
		// Delegate the actual database query to the transaction repository
		List<MerchantHighValueTxnRowDto> transactions = txnRespository.fetchHighValueTxnsByMerchant(merchantId, minAmount, limit, offset);
		return transactions;
	}

}
