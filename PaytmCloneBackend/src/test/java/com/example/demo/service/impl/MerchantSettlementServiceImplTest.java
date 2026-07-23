package com.example.demo.service.impl;

import com.example.demo.dto.MerchantSettlementTransactionDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;
import com.example.demo.entity.Transaction;
import com.example.demo.entity.Wallet;
import com.example.demo.exception.LessAmountException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.factory.TransactionFactory;
import com.example.demo.mapper.SettlementMapper;
import com.example.demo.repository.BankAccountRepository;
import com.example.demo.repository.TransactionRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.validation.KycValidator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * It covers:
	getSettlementHistory()
		 wallet exists → calls repo and maps to DTO
		wallet missing → ResourceNotFoundException
	
	processInstantSettlement()
		happy path: KYC ok, wallet has >=100, primary bank exists → wallet cleared, bank credited (minus 1% fee), transaction saved
		wallet not found → UserNotFoundException
		amount < 100 → LessAmountException
		primary bank missing → ResourceNotFoundException
		verifies kycValidator.validateKycStatus invoked 
 */
@ExtendWith(MockitoExtension.class)
class MerchantSettlementServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private SettlementMapper settlementMapper;
    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private TransactionFactory transactionFactory;
    @Mock private KycValidator kycValidator;

    @InjectMocks
    private MerchantSettlementServiceImpl service;

    // ---------------------------------------
    // getSettlementHistory
    // ---------------------------------------

    @Test
    void getSettlementHistory_shouldReturnMappedPage_whenWalletExists() throws Exception {
        AppUser user = new AppUser();
        user.setId(1L);

        Wallet wallet = new Wallet();
        wallet.setUser(user);

        Pageable pageable = PageRequest.of(0, 10);

        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        Page<Transaction> txPage = new PageImpl<>(List.of(t1, t2), pageable, 2);

        MerchantSettlementTransactionDto d1 = mock(MerchantSettlementTransactionDto.class);
        MerchantSettlementTransactionDto d2 = mock(MerchantSettlementTransactionDto.class);

        when(walletRepository.findByUser(user)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findSettlementsByWallet(wallet, pageable)).thenReturn(txPage);

        // ✅ robust stubbing
        when(settlementMapper.toDto(any(Transaction.class))).thenReturn(d1, d2);

        Page<MerchantSettlementTransactionDto> result = service.getSettlementHistory(user, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertSame(d1, result.getContent().get(0));
        assertSame(d2, result.getContent().get(1));

        verify(settlementMapper, times(2)).toDto(any(Transaction.class));
    }


    @Test
    void getSettlementHistory_shouldThrow_whenWalletMissing() {
        AppUser user = new AppUser();
        user.setId(1L);

        when(walletRepository.findByUser(user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getSettlementHistory(user, PageRequest.of(0, 10)));

        verify(walletRepository).findByUser(user);
        verifyNoInteractions(transactionRepository, settlementMapper);
    }

    // ---------------------------------------
    // processInstantSettlement
    // ---------------------------------------

    @Test
    void processInstantSettlement_shouldSettleAndSaveTransaction_whenValid() throws Exception {
        AppUser merchant = new AppUser();
        merchant.setId(10L);
        merchant.setEmail("m@example.com");

        Wallet wallet = new Wallet();
        wallet.setUser(merchant);
        wallet.setBalance(new BigDecimal("1000.00"));

        BankAccount primaryBank = new BankAccount();
        primaryBank.setId(55L);
        primaryBank.setUser(merchant);
        primaryBank.setAccountNumber("1234");
        primaryBank.setBalance(new BigDecimal("200.00"));

        // fee = 1% of 1000 = 10
        // settleable = 990
        BigDecimal expectedFee = new BigDecimal("10.0000"); // scale may differ
        BigDecimal expectedSettleable = new BigDecimal("990.00");

        when(walletRepository.findByUser(merchant)).thenReturn(Optional.of(wallet));
        when(bankAccountRepository.findByUserAndIsPrimaryTrueAndActiveTrue(merchant))
                .thenReturn(Optional.of(primaryBank));

        Transaction tx = new Transaction();
        tx.setId(999L);
        when(transactionFactory.createSettlementTransaction(eq(wallet), eq(primaryBank), any(BigDecimal.class)))
                .thenReturn(tx);

        service.processInstantSettlement(merchant);

        // KYC check must happen
        verify(kycValidator).validateKycStatus(merchant);

        // Wallet should be cleared
        assertEquals(BigDecimal.ZERO, wallet.getBalance());

        // Bank should be credited with net amount (we verify delta)
        BigDecimal newBankBalance = primaryBank.getBalance();
        // 200 + 990 = 1190
        assertEquals(new BigDecimal("1190.0000"), newBankBalance);

        // Transaction saved with correct settleable amount and narration contains fee
        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transactionFactory).createSettlementTransaction(eq(wallet), eq(primaryBank), amountCaptor.capture());

        BigDecimal actualSettleable = amountCaptor.getValue();
        // Don’t assert exact scale; compare numeric value:
        assertTrue(actualSettleable.compareTo(expectedSettleable) == 0);

        assertNotNull(tx.getNarration());
        assertTrue(tx.getNarration().contains("Instant Settlement"));
        assertTrue(tx.getNarration().contains("Fee"));

        verify(transactionRepository).save(tx);
    }

    @Test
    void processInstantSettlement_shouldThrow_whenWalletNotFound() throws Exception {
        AppUser merchant = new AppUser();
        merchant.setEmail("m@example.com");

        // KYC passes
        doNothing().when(kycValidator).validateKycStatus(merchant);

        when(walletRepository.findByUser(merchant)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.processInstantSettlement(merchant));

        verify(kycValidator).validateKycStatus(merchant);
        verify(walletRepository).findByUser(merchant);
        verifyNoInteractions(bankAccountRepository, transactionFactory, transactionRepository);
    }

    @Test
    void processInstantSettlement_shouldThrow_whenAmountLessThan100() throws Exception {
        AppUser merchant = new AppUser();
        merchant.setEmail("m@example.com");

        Wallet wallet = new Wallet();
        wallet.setUser(merchant);
        wallet.setBalance(new BigDecimal("99.99"));

        doNothing().when(kycValidator).validateKycStatus(merchant);
        when(walletRepository.findByUser(merchant)).thenReturn(Optional.of(wallet));

        assertThrows(LessAmountException.class,
                () -> service.processInstantSettlement(merchant));

        verify(kycValidator).validateKycStatus(merchant);
        verify(walletRepository).findByUser(merchant);
        verifyNoInteractions(bankAccountRepository, transactionFactory, transactionRepository);
    }

    @Test
    void processInstantSettlement_shouldThrow_whenPrimaryBankMissing() throws Exception {
        AppUser merchant = new AppUser();
        merchant.setEmail("m@example.com");

        Wallet wallet = new Wallet();
        wallet.setUser(merchant);
        wallet.setBalance(new BigDecimal("100.00"));

        doNothing().when(kycValidator).validateKycStatus(merchant);
        when(walletRepository.findByUser(merchant)).thenReturn(Optional.of(wallet));
        when(bankAccountRepository.findByUserAndIsPrimaryTrueAndActiveTrue(merchant))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.processInstantSettlement(merchant));

        verify(kycValidator).validateKycStatus(merchant);
        verify(walletRepository).findByUser(merchant);
        verify(bankAccountRepository).findByUserAndIsPrimaryTrueAndActiveTrue(merchant);
        verifyNoInteractions(transactionFactory, transactionRepository);
    }
}
