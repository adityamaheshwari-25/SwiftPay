package com.example.demo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.demo.dto.MerchantSettlementTransactionDto;
import com.example.demo.entity.AppUser;
import com.example.demo.exception.LessAmountException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UserNotFoundException;

public interface MerchantSettlementService {
    Page<MerchantSettlementTransactionDto> getSettlementHistory(AppUser user, Pageable pageable) throws ResourceNotFoundException;
    // You could add: Optional<MerchantSettlementTransactionDto> getLatestSettlement(String email);
    void processInstantSettlement(AppUser merchant) throws UserNotFoundException, LessAmountException, ResourceNotFoundException;
}
