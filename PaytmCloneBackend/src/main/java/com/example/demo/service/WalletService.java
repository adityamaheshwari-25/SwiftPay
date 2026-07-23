package com.example.demo.service;

import org.springframework.security.core.Authentication;

import com.example.demo.dto.AddMoneyRequestDto;
import com.example.demo.dto.SpendingInsightDto;
import com.example.demo.dto.WalletResponseDto;
import com.example.demo.dto.WalletTransferRequestDto;
import com.example.demo.dto.WalletTransferResponseDto;
import com.example.demo.dto.WithdrawMoneyRequestDto;
import com.example.demo.exception.InActiveBankAccountException;
import com.example.demo.exception.InsufficientBalanceException;
import com.example.demo.exception.InvalidMpinException;
import com.example.demo.exception.KycNotApprovedException;
import com.example.demo.exception.MpinNotSetException;
import com.example.demo.exception.NotVerifiedBankAccountException;
import com.example.demo.exception.ReceiverNotFoundException;
import com.example.demo.exception.ReceiverWalletNotFoundException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.SelfTransferNotAllowedException;
import com.example.demo.exception.SenderWalletNotFoundException;
import com.example.demo.exception.UnauthorizedAccessException;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.exception.WalletNotFoundException;

public interface WalletService {
	WalletResponseDto addMoney(Authentication authentication, AddMoneyRequestDto dto) throws UserNotFoundException, WalletNotFoundException, UnauthorizedAccessException, InActiveBankAccountException, NotVerifiedBankAccountException, InsufficientBalanceException, KycNotApprovedException, ResourceNotFoundException;
	WalletResponseDto withdrawMoney(Authentication authentication, WithdrawMoneyRequestDto dto) throws UserNotFoundException, WalletNotFoundException, UnauthorizedAccessException, InsufficientBalanceException, MpinNotSetException, InvalidMpinException, KycNotApprovedException, ResourceNotFoundException;

	WalletResponseDto viewWallet(Authentication authentication) throws UserNotFoundException, WalletNotFoundException;
	
	WalletTransferResponseDto transferWalletToWallet(Authentication authentication, WalletTransferRequestDto dto) throws SenderWalletNotFoundException, ReceiverNotFoundException, SelfTransferNotAllowedException, MpinNotSetException, UserNotFoundException, InvalidMpinException, ReceiverWalletNotFoundException, InsufficientBalanceException, KycNotApprovedException, ResourceNotFoundException;
	
	SpendingInsightDto getSpendingInsight(Authentication auth) throws UserNotFoundException, ResourceNotFoundException;
}