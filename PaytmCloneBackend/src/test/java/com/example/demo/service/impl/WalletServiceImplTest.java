package com.example.demo.service.impl;

import com.example.demo.dto.*;
import com.example.demo.entity.*;
import com.example.demo.entity.enums.PaymentMode;
import com.example.demo.entity.enums.TransactionStatus;
import com.example.demo.exception.*;
import com.example.demo.factory.TransactionFactory;
import com.example.demo.repository.*;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.*;
import com.example.demo.validation.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *Here, we gonna test:
	addMoney() success
	addMoney() wallet not found
	withdrawMoney() success
	invalid MPIN
	transferWalletToWallet() success
	self transfer
	insufficient balance
	viewWallet()
	getSpendingInsight() logic
	wallet not found in insight

	We will not test:
		@Retryable
		@Idempotent
		@Recover
		optimistic locking behavior
	Those are integration concerns. 
 * 
 */

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private WalletRepository walletRepository;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionService transactionService;
    @Mock private TransactionFactory transactionFactory;
    @Mock private BankAccountValidator bankAccountValidator;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private WalletAmountValidator walletAmountValidator;
    @Mock private KycValidationService kycValidationService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private Authentication auth;

    @InjectMocks
    private WalletServiceImpl service;

    // ----------------------------------------------------
    // ADD MONEY
    // ----------------------------------------------------

    @Test
    void addMoney_shouldUpdateBalances_whenValid() throws Exception {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("1000"));

        BankAccount bank = new BankAccount();
        bank.setBalance(new BigDecimal("5000"));

        AddMoneyRequestDto dto = mock(AddMoneyRequestDto.class);
        when(dto.getAmount()).thenReturn(new BigDecimal("500"));
        when(dto.getBankAccountId()).thenReturn(1L);
        when(dto.getPaymentMode()).thenReturn(PaymentMode.UPI);

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(bankAccountRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(bank));

        when(transactionFactory.createAddMoneyTransaction(any(), any(), any(), any()))
                .thenReturn(new Transaction());

        WalletResponseDto result = service.addMoney(auth, dto);

        assertEquals(new BigDecimal("1500"), wallet.getBalance());
        assertEquals(new BigDecimal("4500"), bank.getBalance());
        verify(transactionService).save(any());
        assertEquals(new BigDecimal("1500"), result.getBalance());
    }

    @Test
    void addMoney_shouldThrow_whenWalletMissing() throws Exception {
        AppUser user = new AppUser();
        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());

        AddMoneyRequestDto dto = mock(AddMoneyRequestDto.class);

        assertThrows(WalletNotFoundException.class,
                () -> service.addMoney(auth, dto));
    }

    // ----------------------------------------------------
    // WITHDRAW MONEY
    // ----------------------------------------------------

    @Test
    void withdrawMoney_shouldUpdateBalances_whenValid() throws Exception {

        AppUser user = new AppUser();
        user.setMpinSet(true);
        user.setMpin("encoded");

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("1000"));

        BankAccount bank = new BankAccount();
        bank.setBalance(new BigDecimal("100"));

        WithdrawMoneyRequestDto dto = mock(WithdrawMoneyRequestDto.class);
        when(dto.getAmount()).thenReturn(new BigDecimal("200"));
        when(dto.getBankAccountId()).thenReturn(1L);
        when(dto.getMpin()).thenReturn("1234");

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(bankAccountRepository.findByIdAndUser(1L, user)).thenReturn(Optional.of(bank));
        when(transactionFactory.createWithdrawTransaction(any(), any(), any()))
                .thenReturn(new Transaction());

        WalletResponseDto result = service.withdrawMoney(auth, dto);

        assertEquals(new BigDecimal("800"), wallet.getBalance());
        assertEquals(new BigDecimal("300"), bank.getBalance());
        verify(transactionService).save(any());
        assertEquals(new BigDecimal("800"), result.getBalance());
    }

    @Test
    void withdrawMoney_shouldThrow_whenInvalidMpin() throws Exception {

        AppUser user = new AppUser();
        user.setMpinSet(true);
        user.setMpin("encoded");

        WithdrawMoneyRequestDto dto = mock(WithdrawMoneyRequestDto.class);
        when(dto.getMpin()).thenReturn("wrong");

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThrows(InvalidMpinException.class,
                () -> service.withdrawMoney(auth, dto));
    }

    // ----------------------------------------------------
    // TRANSFER WALLET
    // ----------------------------------------------------

    @Test
    void transferWalletToWallet_shouldTransfer_whenValid() throws Exception {

        AppUser sender = new AppUser();
        sender.setId(1L);
        sender.setMpinSet(true);
        sender.setMpin("encoded");

        AppUser receiver = new AppUser();
        receiver.setId(2L);

        Wallet senderWallet = new Wallet();
        senderWallet.setBalance(new BigDecimal("1000"));

        Wallet receiverWallet = new Wallet();
        receiverWallet.setBalance(BigDecimal.ZERO);

        WalletTransferRequestDto dto = mock(WalletTransferRequestDto.class);
        when(dto.getReceiverMobile()).thenReturn("9999");
        when(dto.getAmount()).thenReturn(new BigDecimal("200"));
        when(dto.getMpin()).thenReturn("1234");

        when(currentUserService.getCurrentUser(auth)).thenReturn(sender);
        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);
        when(appUserRepository.findByMobile("9999")).thenReturn(Optional.of(receiver));
        when(walletRepository.findByUser(sender)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUser(receiver)).thenReturn(Optional.of(receiverWallet));
        when(transactionFactory.createWalletTransferTransaction(any(), any(), any()))
                .thenReturn(new Transaction());

        WalletTransferResponseDto result =
                service.transferWalletToWallet(auth, dto);

        assertEquals(new BigDecimal("800"), senderWallet.getBalance());
        assertEquals(new BigDecimal("200"), receiverWallet.getBalance());
        verify(transactionService).save(any());
        assertEquals(new BigDecimal("200"), result.getAmountTransferred());
    }

    @Test
    void transferWalletToWallet_shouldThrow_whenSelfTransfer() throws Exception {

        AppUser user = new AppUser();
        user.setId(1L);
        user.setMpinSet(true);
        user.setMpin("encoded");

        WalletTransferRequestDto dto = mock(WalletTransferRequestDto.class);
        when(dto.getReceiverMobile()).thenReturn("9999");
        when(dto.getMpin()).thenReturn("1234");

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(appUserRepository.findByMobile("9999")).thenReturn(Optional.of(user));

        assertThrows(SelfTransferNotAllowedException.class,
                () -> service.transferWalletToWallet(auth, dto));
    }

    // ----------------------------------------------------
    // VIEW WALLET
    // ----------------------------------------------------

    @Test
    void viewWallet_shouldReturnBalance() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("user@example.com");

        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("500"));

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        WalletResponseDto result = service.viewWallet(auth);

        assertEquals(new BigDecimal("500"), result.getBalance());
    }

    // ----------------------------------------------------
    // SPENDING INSIGHT
    // ----------------------------------------------------

    @Test
    void getSpendingInsight_shouldCalculateIncrease() throws Exception {

        AppUser user = new AppUser();
        Wallet wallet = new Wallet();

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));

        when(transactionRepository.sumSpendingByWalletInRange(any(), eq(TransactionStatus.SUCCESS), any(), any()))
                .thenReturn(new BigDecimal("200"))
                .thenReturn(new BigDecimal("100"));

        SpendingInsightDto result = service.getSpendingInsight(auth);

        assertEquals(new BigDecimal("200"), result.getCurrentMonthSpent());
        assertEquals(new BigDecimal("100"), result.getPreviousMonthSpent());
        assertTrue(result.isIncrease());
    }

    @Test
    void getSpendingInsight_shouldThrow_whenWalletMissing() throws Exception {

        AppUser user = new AppUser();
        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getSpendingInsight(auth));
    }
}
