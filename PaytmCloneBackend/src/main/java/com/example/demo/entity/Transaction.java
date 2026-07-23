package com.example.demo.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.PaymentMode;
import com.example.demo.entity.enums.TransactionStatus;
import com.example.demo.entity.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

//Wallet-centric system
@Entity
@Table(name = "transactions")
@Data
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    // Public business ID
    @Column(nullable = false, unique = true)
    private String txId;

    // Logical grouping ID (for future reconciliation)
    @Column(nullable = false)
    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 50) // Increase length to 50
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode paymentMode;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;
    
    
    // used uni-directional relationship only.

    // ---------- SOURCE ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_bank_account_id")
    private BankAccount fromBankAccount;

    // ---------- DESTINATION ----------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_bank_account_id")
    private BankAccount toBankAccount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(length = 255)
    private String narration;

    private LocalDateTime createdAt = LocalDateTime.now();
}
