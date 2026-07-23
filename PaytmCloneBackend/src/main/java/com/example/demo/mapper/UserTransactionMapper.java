package com.example.demo.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.dto.UserTransactionResponseDto;
import com.example.demo.entity.AppUser;
import com.example.demo.entity.Transaction;

@Component
public class UserTransactionMapper {

    public UserTransactionResponseDto mapToDto(Transaction tx, AppUser currentUser) {
        boolean isCredit = isCredit(tx, currentUser);

        return UserTransactionResponseDto.builder()
                .txId(tx.getTxId())
                .referenceId(tx.getReferenceId())
                .transactionType(tx.getTransactionType())
                .paymentMode(tx.getPaymentMode())
                .amount(tx.getAmount())
                .status(tx.getStatus())
                .narration(tx.getNarration())
                .createdAt(tx.getCreatedAt())
                .isCredit(isCredit)
                .counterPartyName(getCounterPartyName(tx, isCredit))
                .build();
    }

    private boolean isCredit(Transaction tx, AppUser user) {
        // If the 'to' wallet belongs to the current user, it's a credit
        return tx.getToWallet() != null &&
               tx.getToWallet().getUser().getId().equals(user.getId());
    }

    private String getCounterPartyName(Transaction tx, boolean isCredit) {
        if (isCredit) {
            // Money came from someone else
            return (tx.getFromWallet() != null) ? tx.getFromWallet().getUser().getName() : "Bank/External";
        } else {
            // Money went to someone else
            return (tx.getToWallet() != null) ? tx.getToWallet().getUser().getName() : "Self/Withdrawal";
        }
    }
}
