package com.example.demo.entity;


import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "audit_logs")
@Data
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auditlog_id")
    private Long id;

    // Who performed the action (nullable for unauthenticated attempts)
    private Long actorUserId;

    // USER / MERCHANT / ADMIN
    private String actorRole;

    // LOGIN, ADD_MONEY, WALLET_TRANSFER, WITHDRAW, etc.
    @Column(nullable = false)
    private String action;

    // SUCCESS / FAILURE
    @Column(nullable = false)
    private String outcome;

    // Reason in case of failure
    @Column(columnDefinition = "TEXT")
    private String failureReason;

    // API endpoint
    private String endpoint;

    // HTTP method
    private String httpMethod;

    // Client IP address
    private String ipAddress;

    private LocalDateTime createdAt = LocalDateTime.now();
}

