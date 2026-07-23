package com.example.demo.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import com.example.demo.dto.BankAccountResponseDto;
import com.example.demo.dto.KycStatusResponseDto;
import com.example.demo.dto.SecurityStatusResponseDto;
import com.example.demo.dto.SpendingInsightDto;
import com.example.demo.dto.UserDashboardResponseDto;
import com.example.demo.dto.UserLookupResponseDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.dto.UserTransactionResponseDto;
import com.example.demo.dto.WalletResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.KycDocument;
import com.example.demo.entity.enums.KycStatus;
import com.example.demo.entity.enums.Role;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.KycDocumentRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.BankAccountService;
import com.example.demo.service.TransactionService;
import com.example.demo.service.WalletService;

/**
 *	@Mock creates a fake repository
	@InjectMocks creates the service and injects mocks into it
	when(...).thenReturn(...) sets mock behavior
	verify(...) ensures a dependency was called (interaction with the dependency happened or not)
	
	Naming convention: methodName_expectedBehavior_whenCondition
	
	MockitoExtension is the bridge between JUnit 5 and Mockito.
	
	@ExtendWith(MockitoExtension.class) tells JUnit 5 to use Mockito’s extension so Mockito annotations like @Mock and @InjectMocks are processed automatically—no manual initialization needed.
	
	Mockito helps isolate the unit under test by replacing dependencies with mocks, enabling fast, deterministic tests.
	
	So mockito is for mocking the repo, services, external clients and all, its the Junit that performs the test.
 
 *A failure occurs when an assertion fails — meaning the expected and actual results differ.
An error occurs when the test cannot complete due to an unexpected exception during execution.
 */
@ExtendWith(MockitoExtension.class)
class AppUserServiceImplTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private KycDocumentRepository kycDocumentRepository;
    @Mock private WalletService walletService;
    @Mock private BankAccountService bankAccountService;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private AppUserServiceImpl appUserService;

    private Authentication auth;

    @BeforeEach
    void setUp() {
        auth = mock(Authentication.class);
    }

    // -------------------------
    // lookupByMobile
    // -------------------------

    @Test
    void lookupByMobile_shouldReturnUserDetails_whenUserExists_nonMerchant() throws Exception {
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(10L);
        when(user.getName()).thenReturn("Aditya");
        when(user.getMobile()).thenReturn("9999999999");
        when(user.getRole()).thenReturn(Role.USER);
        when(user.isActive()).thenReturn(true);
        when(user.isKycVerified()).thenReturn(false);

        when(appUserRepository.findByMobile("9999999999")).thenReturn(Optional.of(user));

        UserLookupResponseDto dto = appUserService.lookupByMobile("9999999999");

        assertNotNull(dto);
        assertEquals(10L, dto.getUserId());
        assertEquals("Aditya", dto.getDisplayName()); // displayName
        assertEquals("9999999999", dto.getMobile());
        assertEquals(Role.USER, dto.getRole());
        assertTrue(dto.isActive());
        assertFalse(dto.isKycVerified());
        assertNull(dto.getMerchantCode());

        verify(appUserRepository).findByMobile("9999999999");
        verifyNoMoreInteractions(appUserRepository); // It’s used to enforce that no unexpected interactions happened with dependencies; it helps detect accidental extra calls and keeps unit tests strict.
    }

    @Test
    void lookupByMobile_shouldReturnMerchantBusinessNameAndCode_whenRoleIsMerchant() throws Exception {
        // Deep stubs allow: user.getMerchant().getBusinessName() without building Merchant object manually
        AppUser user = mock(AppUser.class, RETURNS_DEEP_STUBS);
        when(user.getId()).thenReturn(11L);
        when(user.getMobile()).thenReturn("8888888888");
        when(user.getRole()).thenReturn(Role.MERCHANT);
        when(user.isActive()).thenReturn(true);
        when(user.isKycVerified()).thenReturn(true);

        when(user.getMerchant().getBusinessName()).thenReturn("My Shop Pvt Ltd");
        when(user.getMerchant().getMerchantCode()).thenReturn("MRC123");

        when(appUserRepository.findByMobile("8888888888")).thenReturn(Optional.of(user));

        UserLookupResponseDto dto = appUserService.lookupByMobile("8888888888");

        assertNotNull(dto);
        assertEquals(11L, dto.getUserId());
        assertEquals("My Shop Pvt Ltd", dto.getDisplayName()); // displayName changes for merchant
        assertEquals("MRC123", dto.getMerchantCode());

        verify(appUserRepository).findByMobile("8888888888");
    }

    @Test
    void lookupByMobile_shouldThrowUserNotFoundException_whenUserMissing() {
        when(appUserRepository.findByMobile("7777777777")).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> appUserService.lookupByMobile("7777777777"));

        assertEquals(ErrorMessage.USER_NOT_FOUND.name(), ex.getMessage());
        verify(appUserRepository).findByMobile("7777777777");
    }

    // -------------------------
    // getKycStatus
    // -------------------------

    @Test
    void getKycStatus_shouldReturnKycStatus_whenDocumentExists() throws Exception {
        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn("user@example.com");

        KycDocument kyc = mock(KycDocument.class);
        when(kyc.getStatus()).thenReturn(KycStatus.APPROVED);
        when(kyc.getRejectionReason()).thenReturn(null);

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(kycDocumentRepository.findByUser(user)).thenReturn(Optional.of(kyc));

        KycStatusResponseDto dto = appUserService.getKycStatus(auth);

        assertNotNull(dto);
        assertEquals(KycStatus.APPROVED, dto.getStatus());
        assertNull(dto.getRejectionReason());

        verify(currentUserService).getCurrentUser(auth);
        verify(kycDocumentRepository).findByUser(user);
    }

    @Test
    void getKycStatus_shouldReturnNotApplied_whenNoDocument() throws Exception {
        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn("user@example.com");

        when(currentUserService.getCurrentUser(auth)).thenReturn(user);
        when(kycDocumentRepository.findByUser(user)).thenReturn(Optional.empty());

        KycStatusResponseDto dto = appUserService.getKycStatus(auth);

        assertNotNull(dto);
        assertEquals(KycStatus.NOT_APPLIED, dto.getStatus());
        assertNull(dto.getRejectionReason());

        verify(currentUserService).getCurrentUser(auth);
        verify(kycDocumentRepository).findByUser(user);
    }

    // -------------------------
    // getSecurityStatus
    // -------------------------

    @Test
    void getSecurityStatus_shouldReturnMpinAndKyc_whenKycExists() throws Exception {
        when(auth.getName()).thenReturn("user@example.com");

        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.isMpinSet()).thenReturn(true);

        KycDocument kyc = mock(KycDocument.class);
        when(kyc.getStatus()).thenReturn(KycStatus.REJECTED);
        when(kyc.getRejectionReason()).thenReturn("Blurry document");

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(kycDocumentRepository.findByUser(user)).thenReturn(Optional.of(kyc));

        SecurityStatusResponseDto dto = appUserService.getSecurityStatus(auth);

        assertNotNull(dto);
        assertTrue(dto.isMpinSet());
        assertEquals(KycStatus.REJECTED, dto.getKycStatus());
        assertEquals("Blurry document", dto.getRejectionReason());

        verify(appUserRepository).findByEmail("user@example.com");
        verify(kycDocumentRepository).findByUser(user);
    }

    @Test
    void getSecurityStatus_shouldReturnNotApplied_whenNoKycDoc() throws Exception {
        when(auth.getName()).thenReturn("user@example.com");

        AppUser user = mock(AppUser.class);
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.isMpinSet()).thenReturn(false);

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(kycDocumentRepository.findByUser(user)).thenReturn(Optional.empty());

        SecurityStatusResponseDto dto = appUserService.getSecurityStatus(auth);

        assertNotNull(dto);
        assertFalse(dto.isMpinSet());
        assertEquals(KycStatus.NOT_APPLIED, dto.getKycStatus());
        assertNull(dto.getRejectionReason());

        verify(appUserRepository).findByEmail("user@example.com");
        verify(kycDocumentRepository).findByUser(user);
    }

    // -------------------------
    // getMyProfile
    // -------------------------

    @Test
    void getMyProfile_shouldReturnProfile_whenUserExists() throws Exception {
        when(auth.getName()).thenReturn("user@example.com");

        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(5L);
        when(user.getName()).thenReturn("Aditya");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getMobile()).thenReturn("9999999999");
        when(user.getRole()).thenReturn(Role.USER);

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        UserResponseDto dto = appUserService.getMyProfile(auth);

        assertNotNull(dto);
        assertEquals(5L, dto.getId());
        assertEquals("Aditya", dto.getName());
        assertEquals("user@example.com", dto.getEmail());
        assertEquals("9999999999", dto.getPhoneNumber());
        assertEquals(Role.USER.name(), dto.getRole());

        verify(appUserRepository).findByEmail("user@example.com");
    }

    @Test
    void getMyProfile_shouldThrow_whenUserNotFound() {
        when(auth.getName()).thenReturn("missing@example.com");
        when(appUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(UserNotFoundException.class,
                () -> appUserService.getMyProfile(auth));

        assertEquals(ErrorMessage.USER_NOT_FOUND.name(), ex.getMessage());
        verify(appUserRepository).findByEmail("missing@example.com");
    }

    // -------------------------
    // getUserDashboard
    // -------------------------

    @Test
    void getUserDashboard_shouldBuildDashboard_whenAllDependenciesReturnData() throws Exception {
        when(auth.getName()).thenReturn("user@example.com");

        // Profile part depends on appUserRepository
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("Aditya");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getMobile()).thenReturn("9999999999");
        when(user.getRole()).thenReturn(Role.USER);
        when(user.isMpinSet()).thenReturn(true);

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(kycDocumentRepository.findByUser(user)).thenReturn(Optional.empty()); // for getSecurityStatus

        WalletResponseDto wallet = mock(WalletResponseDto.class);
        List<BankAccountResponseDto> bankAccounts = List.of(mock(BankAccountResponseDto.class));

        Page<UserTransactionResponseDto> txPage =
                new PageImpl<>(List.of(mock(UserTransactionResponseDto.class)), PageRequest.of(0, 10), 1);

        SpendingInsightDto spending = mock(SpendingInsightDto.class);

        when(walletService.viewWallet(auth)).thenReturn(wallet);
        when(bankAccountService.getMyBankAccounts("user@example.com")).thenReturn(bankAccounts);
        when(transactionService.getMyTransactions(eq(auth), any(PageRequest.class))).thenReturn(txPage);
        when(walletService.getSpendingInsight(auth)).thenReturn(spending);

        UserDashboardResponseDto dashboard = appUserService.getUserDashboard(auth);

        assertNotNull(dashboard);
        assertNotNull(dashboard.getProfile());
        assertNotNull(dashboard.getWallet());
        assertNotNull(dashboard.getBankAccounts());
        assertNotNull(dashboard.getTransactions());
        assertNotNull(dashboard.getSecurity());
        assertNotNull(dashboard.getSpending());

        // Verify key interactions (this is where verify helps)
        verify(appUserRepository, times(2)).findByEmail("user@example.com"); // getMyProfile + getSecurityStatus
        verify(walletService).viewWallet(auth);
        verify(bankAccountService).getMyBankAccounts("user@example.com");
        verify(transactionService).getMyTransactions(eq(auth), any(PageRequest.class));
        verify(walletService).getSpendingInsight(auth);
        verify(kycDocumentRepository).findByUser(user);
    }

    @Test
    void getUserDashboard_shouldThrow_whenWalletServiceThrows() throws Exception {
        when(auth.getName()).thenReturn("user@example.com");

        // getMyProfile needs user
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(1L);
        when(user.getName()).thenReturn("Aditya");
        when(user.getEmail()).thenReturn("user@example.com");
        when(user.getMobile()).thenReturn("9999999999");
        when(user.getRole()).thenReturn(Role.USER);

        when(appUserRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        when(walletService.viewWallet(auth)).thenThrow(new WalletNotFoundException(ErrorMessage.WALLET_NOT_FOUND));

        assertThrows(WalletNotFoundException.class, () -> appUserService.getUserDashboard(auth));

        verify(appUserRepository).findByEmail("user@example.com");
        verify(walletService).viewWallet(auth);
        // No need to verify others—they shouldn't be reached reliably after exception
    }
}

