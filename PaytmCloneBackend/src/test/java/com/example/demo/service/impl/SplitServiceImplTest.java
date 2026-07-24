package com.example.demo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.example.demo.dto.SplitCreateRequestDto;
import com.example.demo.dto.SplitCreatedListItemDto;
import com.example.demo.dto.SplitDetailsResponseDto;
import com.example.demo.dto.SplitInvolvedListItemDto;
import com.example.demo.dto.SplitPayRequestDto;
import com.example.demo.dto.SplitPayResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.SplitParticipant;
import com.example.demo.entity.SplitRequest;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.enums.Role;
import com.example.demo.entity.enums.SplitParticipantState;
import com.example.demo.entity.enums.SplitStatus;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.MembersAreRequiredException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.factory.TransactionFactory;
import com.example.demo.mapper.SplitMapper;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.SplitParticipantRespository;
import com.example.demo.repository.SplitRequestRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.KycValidationService;
import com.example.demo.service.SseEventService;
import com.example.demo.service.TransactionService;
import com.example.demo.validation.WalletAmountValidator;

/**
 * We will test:
 *	createSplit() (happy path + validation)
	getSplit() authorization
	payShare() (happy path + MPIN + insufficient cases)
	listCreated()
	listInvolved() 
	
	What We Did NOT Test (On Purpose)
	We didn’t test:
		Idempotent annotation
		Retryable annotation
		SSE behavior deeply
		Optimistic locking
		Those are integration concerns.
		
		
	How do you test a complex orchestration service?
	I isolate business branches, mock all collaborators, verify state transitions, and avoid testing framework-level features like retry or idempotency in unit tests.
 */
@ExtendWith(MockitoExtension.class)
class SplitServiceImplTest {

    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private CurrentUserService currentUserService;
    @Mock private KycValidationService kycValidationService;
    @Mock private AppUserRepository appUserRepository;
    @Mock private SplitRequestRepository splitRequestRepository;
    @Mock private SseEventService sseEventService;
    @Mock private SplitParticipantRespository splitParticipantRespository;
    @Mock private WalletRepository walletRepository;
    @Mock private WalletAmountValidator walletAmountValidator;
    @Mock private TransactionFactory transactionFactory;
    @Mock private TransactionService transactionService;
    @Mock private Authentication auth;
    @Spy private SplitMapper splitMapper = new SplitMapper();

    @InjectMocks
    private SplitServiceImpl service;

    // ------------------------------------------------------
    // CREATE SPLIT
    // ------------------------------------------------------

    @Test
    void createSplit_shouldCreateSplit_whenValid() {

        AppUser initiator = new AppUser();
        initiator.setId(1L);

        AppUser member = new AppUser();
        member.setId(2L);
        member.setMobile("9999");
        member.setRole(Role.USER);

        SplitCreateRequestDto dto = mock(SplitCreateRequestDto.class);
        when(dto.getMemberMobiles()).thenReturn(List.of("9999"));
        when(dto.getAmount()).thenReturn(new BigDecimal("200.00"));
        when(dto.getNote()).thenReturn("Dinner");

        when(currentUserService.getCurrentUser(auth)).thenReturn(initiator);
        when(appUserRepository.findByMobile("9999")).thenReturn(Optional.of(member));
        when(splitRequestRepository.save(any())).thenAnswer(inv -> {
            SplitRequest sr = inv.getArgument(0);
            sr.setId(100L);
            return sr;
        });

        SplitDetailsResponseDto result = service.createSplit(auth, dto);

        assertNotNull(result);
        assertEquals(100L, result.getSplitId());
        verify(splitRequestRepository).save(any());
        verify(sseEventService, atLeastOnce()).sendToUser(anyLong(), eq("split.created"), any());
    }

    @Test
    void createSplit_shouldThrow_whenMembersEmpty() {
        SplitCreateRequestDto dto = mock(SplitCreateRequestDto.class);
        when(dto.getMemberMobiles()).thenReturn(Collections.emptyList());
        when(dto.getAmount()).thenReturn(new BigDecimal("200.00"));

        when(currentUserService.getCurrentUser(auth)).thenReturn(new AppUser());

        assertThrows(MembersAreRequiredException.class,
                () -> service.createSplit(auth, dto));
    }

    // ------------------------------------------------------
    // GET SPLIT
    // ------------------------------------------------------

    @Test
    void getSplit_shouldReturn_whenUserIsParticipant() {

        AppUser user = new AppUser();
        user.setId(1L);

        SplitRequest sr = new SplitRequest();
        sr.setId(50L);

        sr.setInitiator(user);
        sr.setParticipants(new ArrayList<>());

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(splitRequestRepository.findById(50L)).thenReturn(Optional.of(sr));

        SplitDetailsResponseDto result = service.getSplit(auth, 50L);

        assertNotNull(result);
        assertEquals(50L, result.getSplitId());
    }

    @Test
    void getSplit_shouldThrow_whenNotParticipant() {
        AppUser user = new AppUser();
        user.setId(1L);

        AppUser other = new AppUser();
        other.setId(2L);

        SplitRequest sr = new SplitRequest();
        sr.setInitiator(other);
        sr.setParticipants(new ArrayList<>());

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(splitRequestRepository.findById(10L)).thenReturn(Optional.of(sr));

        assertThrows(ResourceNotFoundException.class,
                () -> service.getSplit(auth, 10L));
    }

    // ------------------------------------------------------
    // PAY SHARE
    // ------------------------------------------------------

    @Test
    void payShare_shouldProcessPayment_whenValid() {

        AppUser payer = new AppUser();
        payer.setId(1L);
        payer.setMpinSet(true);
        payer.setMpin("encoded");

        SplitRequest sr = new SplitRequest();
        sr.setId(10L);
        sr.setInitiator(new AppUser());

        SplitParticipant sp = new SplitParticipant();
        sp.setState(SplitParticipantState.PENDING);
        sp.setShareAmount(new BigDecimal("100"));
        sp.setSplitRequest(sr);
        sp.setParticipant(payer);

        Wallet payerWallet = new Wallet();
        payerWallet.setBalance(new BigDecimal("500"));

        Wallet initiatorWallet = new Wallet();
        initiatorWallet.setBalance(BigDecimal.ZERO);

        Transaction tx = new Transaction();
        tx.setTxId("TX123");

        when(currentUserService.getCurrentUser(auth)).thenReturn(payer);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(splitRequestRepository.findById(10L)).thenReturn(Optional.of(sr));
        when(splitParticipantRespository.findBySplitRequest_IdAndParticipant_Id(10L, 1L))
                .thenReturn(Optional.of(sp));
        when(walletRepository.findByUser(payer)).thenReturn(Optional.of(payerWallet));
        when(walletRepository.findByUser(sr.getInitiator())).thenReturn(Optional.of(initiatorWallet));
        when(transactionFactory.createSplitPaymentTransaction(any(), any(), any(), any()))
                .thenReturn(tx);

        SplitPayRequestDto dto = mock(SplitPayRequestDto.class);
        when(dto.getMpin()).thenReturn("1234");

        SplitPayResponseDto result = service.payShare(auth, 10L, dto);

        assertEquals("TX123", result.getTxId());
        assertEquals(SplitParticipantState.PAID, sp.getState());
        verify(transactionService).save(tx);
        verify(splitParticipantRespository).save(sp);
    }

    @Test
    void payShare_shouldThrow_whenMpinInvalid() {

        AppUser payer = new AppUser();
        payer.setId(1L);
        payer.setMpinSet(true);
        payer.setMpin("encoded");

        when(currentUserService.getCurrentUser(auth)).thenReturn(payer);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        SplitPayRequestDto dto = mock(SplitPayRequestDto.class);
        when(dto.getMpin()).thenReturn("wrong");

        assertThrows(InvalidMpinException.class,
                () -> service.payShare(auth, 1L, dto));
    }

    // ------------------------------------------------------
    // LIST CREATED
    // ------------------------------------------------------

    @Test
    void listCreated_shouldReturnList() {

        AppUser user = new AppUser();
        user.setId(1L);

        SplitRequest sr = new SplitRequest();
        sr.setId(10L);
        sr.setParticipants(new ArrayList<>());

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(splitRequestRepository.findByInitiator_IdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(sr));

        List<SplitCreatedListItemDto> result = service.listCreated(auth);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getSplitId());
    }

    // ------------------------------------------------------
    // LIST INVOLVED
    // ------------------------------------------------------

    @Test
    void listInvolved_shouldReturnPendingOnly() {

        AppUser user = new AppUser();
        user.setId(1L);

        SplitRequest sr = new SplitRequest();
        sr.setId(10L);
        sr.setStatus(SplitStatus.OPEN);
        sr.setInitiator(user);
        sr.setParticipants(new ArrayList<>());
        sr.setCreatedAt(LocalDateTime.now());

        SplitParticipant sp = new SplitParticipant();
        sp.setState(SplitParticipantState.PENDING);
        sp.setSplitRequest(sr);
        sp.setParticipant(user);
        sp.setShareAmount(new BigDecimal("100"));

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(splitParticipantRespository.findAllForUser(1L))
                .thenReturn(List.of(sp));

        List<SplitInvolvedListItemDto> result = service.listInvolved(auth);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getSplitId());
    }

}
