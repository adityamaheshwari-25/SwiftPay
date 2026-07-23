package com.example.demo.service.impl;

import com.example.demo.dto.SetMpinRequestDto;
import com.example.demo.dto.VerifyMpinRequestDto;
import com.example.demo.entity.AppUser;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.security.CurrentUserService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 *We will test:
	
	setMpin()
		encodes MPIN
		sets mpinSet = true
		saves user
	
	verifyMpin()
		throws MpinNotSetException if not set
		throws InvalidMpinException if wrong MPIN
		succeeds if correct MPIN 
		
		
	How would you test password validation logic?
	I mock BCryptPasswordEncoder and control its matches() behavior to simulate correct and incorrect MPIN scenarios, while verifying that validation logic behaves correctly.
 
 *What is ArgumentCaptor used for?
 *ArgumentCaptor allows capturing arguments passed to mocked methods so that we can inspect their internal state during verification.
 */

@ExtendWith(MockitoExtension.class)
class MpinServiceImplTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AppUserRepository userRepository;
    @Mock private Authentication authentication;

    @InjectMocks
    private MpinServiceImpl service;

    // ---------------------------------------
    // setMpin
    // ---------------------------------------

    @Test
    void setMpin_shouldEncodeAndSaveUser() throws Exception {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");

        SetMpinRequestDto dto = mock(SetMpinRequestDto.class);
        when(dto.getMpin()).thenReturn("1234");

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);
        when(passwordEncoder.encode("1234")).thenReturn("encoded-mpin");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);

        service.setMpin(authentication, dto);

        verify(passwordEncoder).encode("1234");
        
        /*
         *	Service modifies user → calls save(user)
                ↓
			ArgumentCaptor catches user at save()
			                ↓
			Test inspects user’s final state 
         **/
        verify(userRepository).save(captor.capture());

        AppUser savedUser = captor.getValue();
        assertEquals("encoded-mpin", savedUser.getMpin());
        assertTrue(savedUser.isMpinSet());
    }

    // ---------------------------------------
    // verifyMpin
    // ---------------------------------------

    @Test
    void verifyMpin_shouldThrow_whenMpinNotSet() {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setMpinSet(false);

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);

        VerifyMpinRequestDto dto = mock(VerifyMpinRequestDto.class);

        assertThrows(MpinNotSetException.class,
                () -> service.verifyMpin(authentication, dto));

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void verifyMpin_shouldThrow_whenInvalidMpin() {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setMpinSet(true);
        user.setMpin("encoded");

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);

        VerifyMpinRequestDto dto = mock(VerifyMpinRequestDto.class);
        when(dto.getMpin()).thenReturn("wrong");

        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThrows(InvalidMpinException.class,
                () -> service.verifyMpin(authentication, dto));

        verify(passwordEncoder).matches("wrong", "encoded");
    }

    @Test
    void verifyMpin_shouldSucceed_whenCorrectMpin() throws Exception {

        AppUser user = new AppUser();
        user.setEmail("user@example.com");
        user.setMpinSet(true);
        user.setMpin("encoded");

        when(currentUserService.getCurrentUser(authentication)).thenReturn(user);

        VerifyMpinRequestDto dto = mock(VerifyMpinRequestDto.class);
        when(dto.getMpin()).thenReturn("1234");

        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);

        service.verifyMpin(authentication, dto);

        verify(passwordEncoder).matches("1234", "encoded");
    }
}
