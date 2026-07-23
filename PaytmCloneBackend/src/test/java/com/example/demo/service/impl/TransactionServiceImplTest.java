package com.example.demo.service.impl;

import com.example.demo.dto.MerchantTransactionResponseDto;
import com.example.demo.dto.UserTransactionResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Transaction;
import com.example.demo.mapper.MerchantTransactionMapper;
import com.example.demo.mapper.UserTransactionMapper;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.security.CurrentUserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *These tests cover:
	save() saves and returns transaction
	getMyTransactions() returns mapped user DTO page + verifies mapper called with (tx, user)
	getMerchantTransactions() returns mapped merchant DTO page + verifies merchant mapper called 
 * 
 */

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserTransactionMapper userTransactionMapper;
    @Mock private CurrentUserService currentUserService;
    @Mock private MerchantTransactionMapper merchantTransactionMapper;
    @Mock private Authentication auth;

    @InjectMocks
    private TransactionServiceImpl service;

    // ---------------------------------------
    // save()
    // ---------------------------------------

    @Test
    void save_shouldPersistAndReturnSavedTransaction() {
        Transaction tx = new Transaction();
        Transaction saved = new Transaction();
        saved.setId(100L);

        when(transactionRepository.save(tx)).thenReturn(saved);

        Transaction result = service.save(tx);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(transactionRepository).save(tx);
        verifyNoMoreInteractions(transactionRepository);
    }

    // ---------------------------------------
    // getMyTransactions()
    // ---------------------------------------

    @Test
    void getMyTransactions_shouldReturnMappedDtoPage() throws Exception {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setEmail("user@example.com");

        Pageable pageable = PageRequest.of(0, 2);

        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        Page<Transaction> txPage = new PageImpl<>(List.of(t1, t2), pageable, 2);

        UserTransactionResponseDto d1 = mock(UserTransactionResponseDto.class);
        UserTransactionResponseDto d2 = mock(UserTransactionResponseDto.class);

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(transactionRepository.findUserTransactions(1L, pageable)).thenReturn(txPage);

        // IMPORTANT: map gets called for each tx; easiest is sequential stub
        when(userTransactionMapper.mapToDto(any(Transaction.class), eq(user)))
                .thenReturn(d1, d2);

        Page<UserTransactionResponseDto> result = service.getMyTransactions(auth, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertSame(d1, result.getContent().get(0));
        assertSame(d2, result.getContent().get(1));

        verify(currentUserService).getCurrentUser(auth);
        verify(transactionRepository).findUserTransactions(1L, pageable);
        verify(userTransactionMapper, times(2)).mapToDto(any(Transaction.class), eq(user));
    }

    // ---------------------------------------
    // getMerchantTransactions()
    // ---------------------------------------

    @Test
    void getMerchantTransactions_shouldReturnMappedMerchantDtoPage() throws Exception {
        AppUser merchant = new AppUser();
        merchant.setId(10L);
        merchant.setEmail("merchant@example.com");

        Pageable pageable = PageRequest.of(0, 2);

        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        Page<Transaction> txPage = new PageImpl<>(List.of(t1, t2), pageable, 2);

        MerchantTransactionResponseDto d1 = mock(MerchantTransactionResponseDto.class);
        MerchantTransactionResponseDto d2 = mock(MerchantTransactionResponseDto.class);

        when(currentUserService.getCurrentUser(auth)).thenReturn(merchant);
        when(transactionRepository.findMerchantTransactions(10L, pageable)).thenReturn(txPage);

        when(merchantTransactionMapper.mapToMerchantDto(any(Transaction.class)))
                .thenReturn(d1, d2);

        Page<MerchantTransactionResponseDto> result =
                service.getMerchantTransactions(auth, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertSame(d1, result.getContent().get(0));
        assertSame(d2, result.getContent().get(1));

        verify(currentUserService).getCurrentUser(auth);
        verify(transactionRepository).findMerchantTransactions(10L, pageable);
        verify(merchantTransactionMapper, times(2)).mapToMerchantDto(any(Transaction.class));
    }
}
