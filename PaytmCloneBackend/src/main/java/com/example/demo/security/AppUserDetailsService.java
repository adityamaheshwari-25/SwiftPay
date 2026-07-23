package com.example.demo.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

/**
 *This:
	Fetches user from DB
	
	Wraps it inside AppUserDetails 
	
	Spring Security delegates user loading to UserDetailsService. I implemented it to fetch users from the 
	database using email as the username.
 */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService{
	
	private final AppUserRepository userRepo;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		AppUser user = userRepo.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
		
		return new AppUserDetails(user);
	}
}
