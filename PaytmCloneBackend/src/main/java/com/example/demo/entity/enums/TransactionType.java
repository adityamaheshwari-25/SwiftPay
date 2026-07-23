package com.example.demo.entity.enums;

public enum TransactionType {
    ADD_MONEY,        // Bank → Wallet
    WITHDRAW,         // Wallet → Bank
    WALLET_TRANSFER,  // Wallet → Wallet
    REFUND,
    SETTLEMENT,
    SPLIT
}
