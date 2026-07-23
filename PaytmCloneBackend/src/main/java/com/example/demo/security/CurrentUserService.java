package com.example.demo.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.demo.entity.AppUser;
import com.example.demo.exception.ErrorMessage;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CurrentUserService {
	private final AppUserRepository userRepository;
	
	public AppUser getCurrentUser(Authentication authentication) throws UserNotFoundException {
		return userRepository
				.findByEmail(authentication.getName())
				.orElseThrow(() -> new UserNotFoundException(ErrorMessage.USER_NOT_FOUND));
	}
}
