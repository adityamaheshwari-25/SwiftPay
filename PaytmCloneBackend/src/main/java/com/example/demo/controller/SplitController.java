package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.example.demo.service.SplitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/splits")
@RequiredArgsConstructor
public class SplitController {
	
	private final SplitService splitService;
	
	@PostMapping
	public ResponseEntity<SplitDetailsResponseDto> create (
				@RequestHeader("Idempotency-Key") String key,
	            Authentication auth,
	            @Valid @RequestBody SplitCreateRequestDto dto
			) throws KycNotApprovedException, UserNotFoundException, MembersAreRequiredException, MembersOnlyAllowedException, ResourceNotFoundException, ReceiverNotFoundException {
		SplitDetailsResponseDto response = splitService.createSplit(auth, dto);
		return ResponseEntity.ok(response);
	}
	
	@GetMapping("/{splitId}")
    public ResponseEntity<SplitDetailsResponseDto> get(
            Authentication auth,
            @PathVariable Long splitId
    ) throws ResourceNotFoundException, UserNotFoundException {
		SplitDetailsResponseDto response = splitService.getSplit(auth, splitId);
        return ResponseEntity.ok(response);
    }
	
	@PostMapping("/{splitId}/pay")
    public ResponseEntity<SplitPayResponseDto> pay(
            @RequestHeader("Idempotency-Key") String key,
            Authentication auth,
            @PathVariable Long splitId,
            @Valid @RequestBody SplitPayRequestDto dto
    ) throws KycNotApprovedException, ResourceNotFoundException, WalletNotFoundException, MpinNotSetException, InvalidMpinException, UserNotFoundException, InsufficientBalanceException {
		var response = splitService.payShare(auth, splitId, dto);
        return ResponseEntity.ok(response);
    }
	
	@GetMapping("/me/created")
	public ResponseEntity<List<SplitCreatedListItemDto>> created(Authentication auth) {
	    return ResponseEntity.ok(splitService.listCreated(auth));
	}
	
	@GetMapping("/me/involved")
	public ResponseEntity<List<SplitInvolvedListItemDto>> involved(Authentication auth) {
	    return ResponseEntity.ok(splitService.listInvolved(auth));
	}
}
