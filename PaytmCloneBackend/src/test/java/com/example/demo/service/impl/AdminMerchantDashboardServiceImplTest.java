package com.example.demo.service.impl;

import com.example.demo.dto.HighValueMerchantSummaryDto;
import com.example.demo.dto.MerchantHighValueTxnRowDto;
import com.example.demo.dto.PageResponse;
import com.example.demo.repository.AdminMerchantSummaryRepository;
import com.example.demo.repository.AdminMerchantTxnRespository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *Caching (@Cacheable) is NOT tested in unit tests because caching requires Spring context. 
 *This test focuses on business logic only (which is correct for unit tests).
 * 
 * How do you test caching?
 * I use @SpringBootTest and enable caching, then call the method twice and verify repository is called only once using Mockito verify(times(1)).
 */
@ExtendWith(MockitoExtension.class)
class AdminMerchantDashboardServiceImplTest {

    @Mock
    private AdminMerchantSummaryRepository summaryRepository;

    @Mock
    private AdminMerchantTxnRespository txnRespository;

    @InjectMocks
    private AdminMerchantDashboardServiceImpl service;

    // -----------------------------------------
    // getHighValueMerchantSummary
    // -----------------------------------------

    @Test
    void getHighValueMerchantSummary_shouldReturnPageResponse_withCorrectData() {
        BigDecimal minAmount = new BigDecimal("10000");
        String q = "Shop";
        int limit = 10;
        int offset = 0;

        HighValueMerchantSummaryDto dto = mock(HighValueMerchantSummaryDto.class);
        List<HighValueMerchantSummaryDto> items = List.of(dto);
        long total = 25L;

        when(summaryRepository.fetchSummary(minAmount, q, limit, offset))
                .thenReturn(items);
        when(summaryRepository.fetchTotalMerchants(minAmount, q))
                .thenReturn(total);

        PageResponse<HighValueMerchantSummaryDto> response =
                service.getHighValueMerchantSummary(minAmount, q, limit, offset);

        assertNotNull(response);
        assertEquals(items, response.getItems());
        assertEquals(total, response.getTotal());
        assertEquals(limit, response.getLimit());
        assertEquals(offset, response.getOffset());

        verify(summaryRepository).fetchSummary(minAmount, q, limit, offset);
        verify(summaryRepository).fetchTotalMerchants(minAmount, q);
        verifyNoMoreInteractions(summaryRepository);
    }

    // -----------------------------------------
    // getMerchantHighValueTransactions
    // -----------------------------------------

    @Test
    void getMerchantHighValueTransactions_shouldReturnTransactionList() {
        Long merchantId = 5L;
        BigDecimal minAmount = new BigDecimal("5000");
        int limit = 20;
        int offset = 0;

        MerchantHighValueTxnRowDto txn = mock(MerchantHighValueTxnRowDto.class);
        List<MerchantHighValueTxnRowDto> txns = List.of(txn);

        when(txnRespository.fetchHighValueTxnsByMerchant(
                merchantId, minAmount, limit, offset))
                .thenReturn(txns);

        List<MerchantHighValueTxnRowDto> result =
                service.getMerchantHighValueTransactions(merchantId, minAmount, limit, offset);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(txn, result.get(0));

        verify(txnRespository).fetchHighValueTxnsByMerchant(
                merchantId, minAmount, limit, offset);
        verifyNoMoreInteractions(txnRespository);
    }
}
