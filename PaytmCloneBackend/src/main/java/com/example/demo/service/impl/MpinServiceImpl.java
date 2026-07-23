package com.example.demo.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.demo.dto.SetMpinRequestDto;
import com.example.demo.dto.VerifyMpinRequestDto;
import com.example.demo.entity.AppUser;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AppUserRepository;
import com.example.demo.security.CurrentUserService;
import com.example.demo.service.MpinService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MpinServiceImpl implements MpinService{
	
	private final CurrentUserService currentUserService;
	private final BCryptPasswordEncoder passwordEncoder;
	private final AppUserRepository userRepository;
	
	@Override
	public void setMpin(Authentication authentication, SetMpinRequestDto dto) throws UserNotFoundException {
		
		AppUser user = currentUserService.getCurrentUser(authentication);
		
		log.info("Request to set/update MPIN received for user: {}", user.getEmail());
		
		user.setMpin(passwordEncoder.encode(dto.getMpin()));
		user.setMpinSet(true);

		userRepository.save(user);	
		
		log.info("MPIN successfully set/updated and encrypted for user: {}", user.getEmail());
	}

	@Override
	public void verifyMpin(Authentication authentication, VerifyMpinRequestDto dto) throws UserNotFoundException, MpinNotSetException, InvalidMpinException {
		AppUser user = currentUserService.getCurrentUser(authentication);
		
		log.debug("Initiating MPIN verification for user: {}", user.getEmail());
		
		if (!user.isMpinSet()) {
			throw new MpinNotSetException(ErrorMessage.SET_MPIN_BEFORE_WITHDRAWAL);
		}
		
		if (!passwordEncoder.matches(dto.getMpin(), user.getMpin())) {
			log.warn("Security Event: Invalid MPIN attempt for user: {}", user.getEmail());
			throw new InvalidMpinException(ErrorMessage.INVALID_MPIN);
		}
		
		log.info("MPIN verification successful for user: {}", user.getEmail());
		
	}
	
	
	
}
