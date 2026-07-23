package com.example.demo.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.dto.MerchantTransactionResponseDto;
import com.example.demo.entity.Transaction;

@Component
public class MerchantTransactionMapper {
	public MerchantTransactionResponseDto mapToMerchantDto(Transaction tx) {
        // We extract the sender's name from the "from" source
        String senderName = "Unknown Customer";
        if (tx.getFromWallet() != null) {
            senderName = tx.getFromWallet().getUser().getName();
        } else if (tx.getFromBankAccount() != null) {
            senderName = tx.getFromBankAccount().getUser().getName();
        }

        return MerchantTransactionResponseDto.builder()
                .txId(tx.getTxId())
                .customerName(senderName)
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .paymentMode(tx.getPaymentMode())
                .narration(tx.getNarration())
                .createdAt(tx.getCreatedAt())
                .referenceId(tx.getReferenceId())
                .build();
    }
}
