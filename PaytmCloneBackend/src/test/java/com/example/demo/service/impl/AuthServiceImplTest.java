package com.example.demo.service.impl;

import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.RegisterAppUserDto;
import com.example.demo.dto.RegisterMerchantDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Merchant;
import com.example.demo.entity.Wallet;
import com.example.demo.entity.enums.Role;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.repository.MerchantRepository;
import com.example.demo.repository.WalletRepository;
import com.example.demo.security.JwtUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.modelmapper.ModelMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 *	It covers:
		registerUser() success + duplicate email/mobile
		registerMerchant() success + duplicate email/mobile
		login() success + user not found
		verifies key interactions (save, authenticate, generateToken)
		avoids testing the private createWallet() directly (we verify walletRepo.save(...) instead) 
 */

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AppUserRepository userRepo;
    @Mock private WalletRepository walletRepo;
    @Mock private MerchantRepository merchantRepo;

    @Mock private ModelMapper mapper;
    @Mock private BCryptPasswordEncoder encoder;
    @Mock private AuthenticationManager authManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    // ----------------------------
    // registerUser
    // ----------------------------

    @Test
    void registerUser_shouldCreateUserWalletAndReturnAuthResponse_whenValid() throws Exception {
        RegisterAppUserDto req = mock(RegisterAppUserDto.class);
        when(req.getEmail()).thenReturn("user@example.com");
        when(req.getMobile()).thenReturn("9999999999");
        when(req.getPassword()).thenReturn("plain-pass");
//        when(req.getName()).thenReturn("Aditya"); // unnessary stubbing error, as it is not used in the service.

        when(userRepo.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepo.existsByMobile("9999999999")).thenReturn(false);

        AppUser mappedUser = new AppUser();
        mappedUser.setName("Aditya");
        mappedUser.setEmail("user@example.com");
        mappedUser.setMobile("9999999999");

        when(mapper.map(req, AppUser.class)).thenReturn(mappedUser);
        when(encoder.encode("plain-pass")).thenReturn("hashed-pass");

        // After save, your service logs user.getId(); in real JPA it’s set after save.
        // We simulate that by setting it in an Answer.
        when(userRepo.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        when(jwtUtil.generateToken(any(AppUser.class))).thenReturn("jwt-token");

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

        AuthResponseDto resp = authService.registerUser(req);

        assertNotNull(resp);
        assertEquals("jwt-token", resp.getToken());
        assertEquals(1L, resp.getUserId());
        assertEquals("Aditya", resp.getName());
        assertEquals(Role.USER.name(), resp.getRole());

        verify(userRepo).existsByEmail("user@example.com");
        verify(userRepo).existsByMobile("9999999999");

        verify(userRepo).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        assertEquals("user@example.com", savedUser.getEmail());
        assertEquals("9999999999", savedUser.getMobile());
        assertEquals("hashed-pass", savedUser.getPassword());
        assertEquals(Role.USER, savedUser.getRole());

        verify(walletRepo).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();
        assertSame(savedUser, savedWallet.getUser());

        verify(jwtUtil).generateToken(savedUser);
    }

    @Test
    void registerUser_shouldThrow_whenEmailAlreadyExists() {
        RegisterAppUserDto req = mock(RegisterAppUserDto.class);
        when(req.getEmail()).thenReturn("user@example.com");

        when(userRepo.existsByEmail("user@example.com")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.registerUser(req)
        );

        assertEquals(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS.name(), ex.getMessage());
        verify(userRepo).existsByEmail("user@example.com");
        verify(userRepo, never()).save(any());
        verify(walletRepo, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void registerUser_shouldThrow_whenMobileAlreadyExists() {
        RegisterAppUserDto req = mock(RegisterAppUserDto.class);
        when(req.getEmail()).thenReturn("user@example.com");
        when(req.getMobile()).thenReturn("9999999999");

        when(userRepo.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepo.existsByMobile("9999999999")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.registerUser(req)
        );

        assertEquals(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS.name(), ex.getMessage());
        verify(userRepo).existsByEmail("user@example.com");
        verify(userRepo).existsByMobile("9999999999");
        verify(userRepo, never()).save(any());
        verify(walletRepo, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    // ----------------------------
    // registerMerchant
    // ----------------------------

    @Test
    void registerMerchant_shouldCreateMerchantUserWalletProfile_andReturnAuthResponse() throws Exception {
        RegisterMerchantDto req = mock(RegisterMerchantDto.class);
        when(req.getEmail()).thenReturn("m@example.com");
        when(req.getMobile()).thenReturn("8888888888");
        when(req.getPassword()).thenReturn("plain-pass");
        when(req.getBusinessName()).thenReturn("My Shop");
        when(req.getCategory()).thenReturn("GROCERY");
//        when(req.getName()).thenReturn("Owner Name");  // unnessary stubbing error, as it is not used in the service.

        when(userRepo.existsByEmail("m@example.com")).thenReturn(false);
        when(userRepo.existsByMobile("8888888888")).thenReturn(false);

        AppUser mappedUser = new AppUser();
        mappedUser.setName("Owner Name");
        mappedUser.setEmail("m@example.com");
        mappedUser.setMobile("8888888888");

        when(mapper.map(req, AppUser.class)).thenReturn(mappedUser);
        when(encoder.encode("plain-pass")).thenReturn("hashed-pass");

        when(userRepo.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });

        when(jwtUtil.generateToken(any(AppUser.class))).thenReturn("jwt-merchant");

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        ArgumentCaptor<Merchant> merchantCaptor = ArgumentCaptor.forClass(Merchant.class);

        AuthResponseDto resp = authService.registerMerchant(req);

        assertNotNull(resp);
        assertEquals("jwt-merchant", resp.getToken());
        assertEquals(2L, resp.getUserId());
        assertEquals("Owner Name", resp.getName());
        assertEquals(Role.MERCHANT.name(), resp.getRole());

        verify(userRepo).save(userCaptor.capture());
        AppUser savedUser = userCaptor.getValue();
        assertEquals(Role.MERCHANT, savedUser.getRole());
        assertEquals("hashed-pass", savedUser.getPassword());

        verify(walletRepo).save(walletCaptor.capture());
        assertSame(savedUser, walletCaptor.getValue().getUser());

        verify(merchantRepo).save(merchantCaptor.capture());
        Merchant savedMerchant = merchantCaptor.getValue();
        assertSame(savedUser, savedMerchant.getUser());
        assertEquals("My Shop", savedMerchant.getBusinessName());
        assertEquals("GROCERY", savedMerchant.getCategory());
        // merchantCode is generated statically, so we just assert it was set (non-null) if getter exists
        // If your Merchant has getMerchantCode(), uncomment:
        // assertNotNull(savedMerchant.getMerchantCode());

        verify(jwtUtil).generateToken(savedUser);
    }

    @Test
    void registerMerchant_shouldThrow_whenEmailAlreadyExists() {
        RegisterMerchantDto req = mock(RegisterMerchantDto.class);
        when(req.getEmail()).thenReturn("m@example.com");

        when(userRepo.existsByEmail("m@example.com")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.registerMerchant(req)
        );

        assertEquals(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS.name(), ex.getMessage());
        verify(userRepo).existsByEmail("m@example.com");
        verify(userRepo, never()).save(any());
        verify(walletRepo, never()).save(any());
        verify(merchantRepo, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    @Test
    void registerMerchant_shouldThrow_whenMobileAlreadyExists() {
        RegisterMerchantDto req = mock(RegisterMerchantDto.class);
        when(req.getEmail()).thenReturn("m@example.com");
        when(req.getMobile()).thenReturn("8888888888");

        when(userRepo.existsByEmail("m@example.com")).thenReturn(false);
        when(userRepo.existsByMobile("8888888888")).thenReturn(true);

        UserAlreadyExistsException ex = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.registerMerchant(req)
        );

        assertEquals(ErrorMessage.EMAIL_OR_PHONE_NUMBER_ALREADY_EXISTS.name(), ex.getMessage());
        verify(userRepo).existsByEmail("m@example.com");
        verify(userRepo).existsByMobile("8888888888");
        verify(userRepo, never()).save(any());
        verify(walletRepo, never()).save(any());
        verify(merchantRepo, never()).save(any());
        verify(jwtUtil, never()).generateToken(any());
    }

    // ----------------------------
    // login
    // ----------------------------

    @Test
    void login_shouldAuthenticateAndReturnToken_whenUserExists() throws Exception {
        LoginRequestDto req = mock(LoginRequestDto.class);
        when(req.getEmail()).thenReturn("user@example.com");
        when(req.getPassword()).thenReturn("plain-pass");

        AppUser user = new AppUser();
        user.setId(7L);
        user.setName("Aditya");
        user.setEmail("user@example.com");
        user.setRole(Role.USER);

        // authManager.authenticate(...) returns Authentication, but we don't need it.
        when(userRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("jwt-login");

        AuthResponseDto resp = authService.login(req);

        assertNotNull(resp);
        assertEquals("jwt-login", resp.getToken());
        assertEquals(7L, resp.getUserId());
        assertEquals("Aditya", resp.getName());
        assertEquals(Role.USER.name(), resp.getRole());

        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepo).findByEmail("user@example.com");
        verify(jwtUtil).generateToken(user);
    }

    @Test
    void login_shouldThrowUserNotFound_whenUserMissing() {
        LoginRequestDto req = mock(LoginRequestDto.class);
        when(req.getEmail()).thenReturn("missing@example.com");
        when(req.getPassword()).thenReturn("pass");

        when(userRepo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        UserNotFoundException ex = assertThrows(
                UserNotFoundException.class,
                () -> authService.login(req)
        );

        assertEquals(ErrorMessage.USER_NOT_FOUND.name(), ex.getMessage());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepo).findByEmail("missing@example.com");
        verify(jwtUtil, never()).generateToken(any());
    }
}
