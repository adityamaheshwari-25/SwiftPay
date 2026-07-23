package com.example.demo.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.example.demo.dto.MerchantSettlementTransactionDto;
import com.example.demo.entity.Transaction;

@Component
public class SettlementMapper {

    public MerchantSettlementTransactionDto toDto(Transaction tx) {
        if (tx == null) return null;

        boolean instant = tx.getNarration() != null && tx.getNarration().contains("Instant");
        
        BigDecimal netAmount = tx.getAmount();
        BigDecimal fee = BigDecimal.ZERO;

        if (instant) {
            // If fee was 1%, the netAmount is 99% (0.99) of the original
            // Original = Net / 0.99
            BigDecimal originalAmount = netAmount.divide(new BigDecimal("0.99"), 2, RoundingMode.HALF_UP);
            fee = originalAmount.subtract(netAmount);
        }

        return MerchantSettlementTransactionDto.builder()
                .txId(tx.getTxId())
                .amount(netAmount) // This is what hit the bank
                .fee(fee)          // This is what was deducted
                .destinationBankName(tx.getToBankAccount().getBankName())
                .accountNumberTail(mask(tx.getToBankAccount().getAccountNumber()))
                .utrNumber(tx.getReferenceId())
                .status(tx.getStatus())
                .narration(tx.getNarration())
                .settledAt(tx.getCreatedAt())
                .isInstant(instant)
                .build();
    }

    private String mask(String acc) {
        return (acc != null && acc.length() > 4) 
            ? "****" + acc.substring(acc.length() - 4) 
            : "****";
    }
}