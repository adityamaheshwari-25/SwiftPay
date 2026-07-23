package com.example.demo.service;

import com.example.demo.dto.AuthResponseDto;
import com.example.demo.dto.LoginRequestDto;
import com.example.demo.dto.RegisterAppUserDto;
import com.example.demo.dto.RegisterMerchantDto;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.exception.UserNotFoundException;

public interface AuthService {
	
	AuthResponseDto registerUser(RegisterAppUserDto req) throws UserAlreadyExistsException;
	
	AuthResponseDto registerMerchant(RegisterMerchantDto req) throws UserAlreadyExistsException;
	
	AuthResponseDto login(LoginRequestDto req) throws UserNotFoundException;
	
}
