package com.example.demo.audit;

public enum AuditAction {
    // Auth & User
    LOGIN,
    LOGOUT,
    REGISTER_USER,
    REGISTER_MERCHANT,
    LOOKUP_USER,

    // Wallet Operations
    ADD_MONEY,
    WITHDRAW,
    WALLET_TRANSFER,
    VIEW_WALLET,

    // Bank & KYC
    ADD_BANK_ACCOUNT,
    VIEW_BANK_ACCOUNTS,
    UPLOAD_KYC,
    VIEW_MY_KYC,
    VIEW_KYC_STATUS,
    APPROVE_KYC,
    REJECT_KYC,
    VIEW_PENDING_KYC_LIST,
    VIEW_USER_KYC_ADMIN,
    DOWNLOAD_KYC_FILE,

    // Security
    SET_MPIN,
    VERIFY_MPIN,

    // Transactions
    VIEW_TRANSACTIONS,
    TRANSACTION_FAILED
}	
