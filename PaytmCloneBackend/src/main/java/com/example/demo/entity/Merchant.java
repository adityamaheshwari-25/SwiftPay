package com.example.demo.entity;

import java.time.LocalDateTime;

import com.example.demo.util.IdGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "merchants")
@Data
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "merchant_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "fk_user_id", unique = true)
    private AppUser user;

    @Column(name = "merchant_code", unique = true, nullable = false)
    private String merchantCode;   // MIDxxxxx format

    private String businessName;

    private String category;

    private LocalDateTime createdAt = LocalDateTime.now();
    
    
    @PrePersist
    public void prePersist() {
        if (merchantCode == null) {
            merchantCode = IdGenerator.generateMerchantId();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}