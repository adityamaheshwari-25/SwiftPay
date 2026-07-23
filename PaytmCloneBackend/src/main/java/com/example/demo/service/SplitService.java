package com.example.demo.service;

import java.util.List;

import org.springframework.security.core.Authentication;

import com.example.demo.dto.SplitCreateRequestDto;
import com.example.demo.dto.SplitCreatedListItemDto;
import com.example.demo.dto.SplitDetailsResponseDto;
import com.example.demo.dto.SplitInvolvedListItemDto;
import com.example.demo.dto.SplitPayRequestDto;
import com.example.demo.dto.SplitPayResponseDto;
import com.example.demo.exception.InsufficientBalanceException;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.KycNotApprovedException;
import com.example.demo.exception.MembersAreRequiredException;
import com.example.demo.exception.MembersOnlyAllowedException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.ReceiverNotFoundException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;

public interface SplitService {
	SplitDetailsResponseDto createSplit(Authentication auth, SplitCreateRequestDto dto) throws UserNotFoundException, MembersAreRequiredException, MembersOnlyAllowedException, KycNotApprovedException, ResourceNotFoundException, ReceiverNotFoundException;
	SplitDetailsResponseDto getSplit(Authentication auth, Long splitId) throws ResourceNotFoundException, UserNotFoundException;
	SplitPayResponseDto payShare(Authentication auth, Long splitId, SplitPayRequestDto dto) throws KycNotApprovedException, ResourceNotFoundException, WalletNotFoundException,MpinNotSetException, InvalidMpinException, UserNotFoundException, InsufficientBalanceException;
	
	List<SplitCreatedListItemDto> listCreated(Authentication auth);
	List<SplitInvolvedListItemDto> listInvolved(Authentication auth);
}
