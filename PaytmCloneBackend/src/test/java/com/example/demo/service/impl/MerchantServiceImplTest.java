package com.example.demo.service.impl;

import com.example.demo.dto.*;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Wallet;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.AppUserService;
import com.example.demo.service.BankAccountService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *@InjectMocks creates the real object under test and automatically injects all mocks into it.
 *What @InjectMocks DOES NOT Do
	It does NOT create a mock of the class under test.
	It does NOT turn the class into a spy.
	It does NOT mock methods of that class. 
	
It creates a real object.

@Mock - Creates a fake object
@InjectMocks - Creates real object and injects mocks into it
@Spy - Creates partial mock (real methods run unless stubbed)


Why use @Spy here?
Because I want to stub a method of the class under test while still testing the rest of its logic. @Spy allows partial mocking.

When using @InjectMocks without @Spy, are real methods executed?
Yes. @InjectMocks creates a real instance of the class under test. All its methods execute normally, while its dependencies annotated with @Mock are injected and mocked.
 */

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock private WalletRepository walletRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AppUserService appUserService;
    @Mock private BankAccountService bankAccountService;
    @Mock private Authentication auth;

    @InjectMocks
    private MerchantServiceImpl service;

    // -----------------------------------------
    // getCollectionStats
    // -----------------------------------------

    @Test
    void getCollectionStats_shouldCalculateStatsCorrectly() throws Exception {

        AppUser user = new AppUser();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("5000"));

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        // transactionRepository.sumReceivedByWalletInRange called 4 times in sequence:
        when(transactionRepository.sumReceivedByWalletInRange(
                eq(wallet), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1000"))   // today
                .thenReturn(new BigDecimal("500"))    // yesterday
                .thenReturn(new BigDecimal("3000"))   // this month
                .thenReturn(new BigDecimal("1000"));  // last month

        CollectionStatsResponse response =
                service.getCollectionStats(auth);

        assertEquals(new BigDecimal("1000"), response.getToday());
        assertEquals(new BigDecimal("3000"), response.getMonthly());
        assertEquals(new BigDecimal("5000"), response.getPending());

        // growth = ((1000-500)/500)*100 = 100%
        assertEquals(100.0, response.getDailyGrowthRate());
        // ((3000-1000)/1000)*100 = 200%
        assertEquals(200.0, response.getMonthlyGrowthRate());
    }

    @Test
    void getCollectionStats_shouldHandleZeroPrevious() throws Exception {

        AppUser user = new AppUser();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        when(transactionRepository.sumReceivedByWalletInRange(
                eq(wallet), any(), any()))
                .thenReturn(new BigDecimal("100"))  // today
                .thenReturn(BigDecimal.ZERO)        // yesterday
                .thenReturn(BigDecimal.ZERO)        // this month
                .thenReturn(BigDecimal.ZERO);       // last month

        CollectionStatsResponse response =
                service.getCollectionStats(auth);

        assertEquals(100.0, response.getDailyGrowthRate());
        assertEquals(0.0, response.getMonthlyGrowthRate());
    }

    @Test
    void getCollectionStats_shouldThrow_whenWalletNotFound() throws Exception {
        AppUser user = new AppUser();
        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getCollectionStats(auth));
    }

    // -----------------------------------------
    // getMerchantDashboard
    // -----------------------------------------

    @Test
    void getMerchantDashboard_shouldAggregateAllComponents_withoutSpy() throws Exception {

        // Arrange: set up auth and dependencies so getCollectionStats runs normally
        when(auth.getName()).thenReturn("merchant@example.com");

        AppUser user = new AppUser();
        user.setId(10L);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(new BigDecimal("2000"));

        // Make collection stats calculation deterministic by wiring its dependencies:
        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        // Provide transaction sums in the same sequence getCollectionStats calls them
        when(transactionRepository.sumReceivedByWalletInRange(eq(wallet), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("500"))   // today
                .thenReturn(new BigDecimal("400"))   // yesterday
                .thenReturn(new BigDecimal("1500"))  // this month
                .thenReturn(new BigDecimal("1000")); // last month

        // Other collaborators used by getMerchantDashboard
        UserResponseDto profile = mock(UserResponseDto.class);
        SecurityStatusResponseDto security = mock(SecurityStatusResponseDto.class);
        BankAccountResponseDto bankDto = mock(BankAccountResponseDto.class);

        when(appUserService.getMyProfile(auth)).thenReturn(profile);
        when(bankAccountService.getMyBankAccounts("merchant@example.com")).thenReturn(List.of(bankDto));
        when(appUserService.getSecurityStatus(auth)).thenReturn(security);

        // Act
        MerchantDashboardResponseDto dashboard = service.getMerchantDashboard(auth);

        // Assert aggregation correctness
        assertNotNull(dashboard);
        assertEquals(profile, dashboard.getProfile());
        assertEquals(security, dashboard.getSecurity());
        assertEquals(1, dashboard.getBankAccounts().size());

        // Ensure collection stats were computed (based on sums above)
        assertEquals(new BigDecimal("500"), dashboard.getStats().getToday());
        assertEquals(new BigDecimal("1500"), dashboard.getStats().getMonthly());
        // daily growth ((500-400)/400)*100 = 25%
        assertEquals(25.0, dashboard.getStats().getDailyGrowthRate());
        // monthly ((1500-1000)/1000)*100 = 50%
        assertEquals(50.0, dashboard.getStats().getMonthlyGrowthRate());

        // verify interactions
        verify(currentUserService).getCurrentUser(auth);
        verify(walletRepository).findByUser(user);
        verify(transactionRepository, atLeastOnce()).sumReceivedByWalletInRange(eq(wallet), any(), any());
        verify(appUserService).getMyProfile(auth);
        verify(bankAccountService).getMyBankAccounts("merchant@example.com");
        verify(appUserService).getSecurityStatus(auth);
    }
}
