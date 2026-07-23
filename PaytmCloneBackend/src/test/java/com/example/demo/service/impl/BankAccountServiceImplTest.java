package com.example.demo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.CreateBankAccountDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.BankAccount;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UnauthorizedAccessException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.mapper.BankAccountMapper;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.BankAccountRepository;


/**
 *It covers:
	createBankAccount()
	- first account becomes primary
	- if existing active account exists → new one not primary
	- user not found throws exception
	
	setPrimaryAccount()
	- sets the account primary and saves
	- user not found
	- bank account not found
	- ownership mismatch throws exception
	
	getMyBankAccounts()
	- returns mapped DTO list
	- user not found 
	
	
	For static methods, we have something called Mockito static mocking.
 */

@ExtendWith(MockitoExtension.class)
class BankAccountServiceImplTest {

    @Mock private BankAccountRepository bankAccountRepository;
    @Mock private BankAccountMapper bankAccountMapper;
    @Mock private AppUserRepository appUserRepository;

    @InjectMocks
    private BankAccountServiceImpl bankAccountService;

    // ----------------------------
    // createBankAccount
    // ----------------------------

    @Test
    void createBankAccount_shouldMakePrimary_whenNoExistingActiveAccount() throws Exception {
        String email = "user@example.com";
        CreateBankAccountDto dto = mock(CreateBankAccountDto.class);
        when(dto.getBankName()).thenReturn("HDFC");

        AppUser user = new AppUser();
        user.setId(1L);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bankAccountRepository.existsByUserAndActiveTrue(user)).thenReturn(false);

        BankAccount entity = new BankAccount();
        entity.setUser(user);
        entity.setPrimary(false); // will be overridden

        when(bankAccountMapper.toEntity(dto, user)).thenReturn(entity);

        BankAccount saved = new BankAccount();
        saved.setId(100L);
        saved.setUser(user);
        saved.setPrimary(true);

        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(saved);

        BankAccountResponseDto respDto = mock(BankAccountResponseDto.class);
        when(bankAccountMapper.toResponseDto(saved)).thenReturn(respDto);

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);

        BankAccountResponseDto result = bankAccountService.createBankAccount(email, dto);

        assertNotNull(result);
        assertSame(respDto, result);

        verify(appUserRepository).findByEmail(email);
        verify(bankAccountRepository).existsByUserAndActiveTrue(user);
        verify(bankAccountMapper).toEntity(dto, user);

        verify(bankAccountRepository).save(captor.capture());
        BankAccount toSave = captor.getValue();
        assertTrue(toSave.isPrimary(), "First account should be primary");

        verify(bankAccountMapper).toResponseDto(saved);
    }

    @Test
    void createBankAccount_shouldNotMakePrimary_whenExistingActiveAccountExists() throws Exception {
        String email = "user@example.com";
        CreateBankAccountDto dto = mock(CreateBankAccountDto.class);
        when(dto.getBankName()).thenReturn("ICICI");

        AppUser user = new AppUser();
        user.setId(1L);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bankAccountRepository.existsByUserAndActiveTrue(user)).thenReturn(true);

        BankAccount entity = new BankAccount();
        entity.setUser(user);

        when(bankAccountMapper.toEntity(dto, user)).thenReturn(entity);

        BankAccount saved = new BankAccount();
        saved.setId(101L);
        saved.setUser(user);
        saved.setPrimary(false);

        when(bankAccountRepository.save(any(BankAccount.class))).thenReturn(saved);

        BankAccountResponseDto respDto = mock(BankAccountResponseDto.class);
        when(bankAccountMapper.toResponseDto(saved)).thenReturn(respDto);

        ArgumentCaptor<BankAccount> captor = ArgumentCaptor.forClass(BankAccount.class);

        BankAccountResponseDto result = bankAccountService.createBankAccount(email, dto);

        assertNotNull(result);

        verify(bankAccountRepository).save(captor.capture());
        BankAccount toSave = captor.getValue();
        assertFalse(toSave.isPrimary(), "If an active account exists, new one must not be primary");
    }

    @Test
    void createBankAccount_shouldThrow_whenUserNotFound() {
        String email = "missing@example.com";
        CreateBankAccountDto dto = mock(CreateBankAccountDto.class);
        when(dto.getBankName()).thenReturn("SBI");

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> bankAccountService.createBankAccount(email, dto));

        // Message assertion might vary in your project; type assertion is enough.
        assertNotNull(ex);

        verify(appUserRepository).findByEmail(email);
        verifyNoInteractions(bankAccountRepository, bankAccountMapper);
    }

    // ----------------------------
    // setPrimaryAccount
    // ----------------------------

    @Test
    void setPrimaryAccount_shouldMarkRequestedAccountPrimary_whenOwnershipMatches() throws Exception {
        String email = "user@example.com";
        Long accountId = 500L;

        AppUser user = new AppUser();
        user.setId(1L);

        AppUser owner = new AppUser();
        owner.setId(1L);

        BankAccount account = new BankAccount();
        account.setId(accountId);
        account.setUser(owner);
        account.setPrimary(false);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        bankAccountService.setPrimaryAccount(email, accountId);

        verify(appUserRepository).findByEmail(email);
        verify(bankAccountRepository).markAllAsNonPrimary(user);
        verify(bankAccountRepository).findById(accountId);

        assertTrue(account.isPrimary(), "Account should be set to primary");
        verify(bankAccountRepository).save(account);
    }

    @Test
    void setPrimaryAccount_shouldThrow_whenUserNotFound() {
        String email = "missing@example.com";
        Long accountId = 500L;

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> bankAccountService.setPrimaryAccount(email, accountId));

        verify(appUserRepository).findByEmail(email);
        verifyNoInteractions(bankAccountRepository);
    }

    @Test
    void setPrimaryAccount_shouldThrow_whenAccountNotFound() {
        String email = "user@example.com";
        Long accountId = 999L;

        AppUser user = new AppUser();
        user.setId(1L);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bankAccountService.setPrimaryAccount(email, accountId));

        verify(bankAccountRepository).markAllAsNonPrimary(user);
        verify(bankAccountRepository).findById(accountId);
        verify(bankAccountRepository, never()).save(any());
    }

    @Test
    void setPrimaryAccount_shouldThrow_whenOwnershipMismatch() {
        String email = "user@example.com";
        Long accountId = 500L;

        AppUser user = new AppUser();
        user.setId(1L);

        AppUser otherOwner = new AppUser();
        otherOwner.setId(2L);

        BankAccount account = new BankAccount();
        account.setId(accountId);
        account.setUser(otherOwner);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(UnauthorizedAccessException.class,
                () -> bankAccountService.setPrimaryAccount(email, accountId));

        verify(bankAccountRepository).markAllAsNonPrimary(user);
        verify(bankAccountRepository).findById(accountId);
        verify(bankAccountRepository, never()).save(any());
    }

    // ----------------------------
    // getMyBankAccounts
    // ----------------------------

    @Test
    void getMyBankAccounts_shouldReturnList_whenUserExists() throws Exception {
        String email = "user@example.com";

        AppUser user = new AppUser();
        user.setId(1L);

        BankAccount a1 = new BankAccount();
        a1.setId(1L);
        a1.setUser(user);

        BankAccount a2 = new BankAccount();
        a2.setId(2L);
        a2.setUser(user);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(bankAccountRepository.findByUser(user)).thenReturn(List.of(a1, a2));

        List<BankAccountResponseDto> result = bankAccountService.getMyBankAccounts(email);

        assertNotNull(result);
        assertEquals(2, result.size());

        verify(appUserRepository).findByEmail(email);
        verify(bankAccountRepository).findByUser(user);
    }

    @Test
    void getMyBankAccounts_shouldThrow_whenUserNotFound() {
        String email = "missing@example.com";

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> bankAccountService.getMyBankAccounts(email));

        verify(appUserRepository).findByEmail(email);
        verifyNoInteractions(bankAccountRepository);
    }
}
