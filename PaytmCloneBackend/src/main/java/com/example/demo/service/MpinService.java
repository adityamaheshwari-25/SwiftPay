package com.example.demo.service;

import org.springframework.security.core.Authentication;

import com.example.demo.dto.SetMpinRequestDto;
import com.example.demo.dto.VerifyMpinRequestDto;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.UserNotFoundException;

public interface MpinService {
	void setMpin(Authentication authentication, SetMpinRequestDto dto) throws UserNotFoundException;
	void verifyMpin(Authentication authenticatio, VerifyMpinRequestDto dto) throws UserNotFoundException, MpinNotSetException, InvalidMpinException;
}
