package com.example.demo.entity;

import java.time.LocalDateTime;

import com.example.demo.entity.enums.KycStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "kyc_document")
@Data
public class KycDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kycdocument_id")
    private Long id;

    @OneToOne
    @JoinColumn(name = "fk_user_id", unique = true)
    private AppUser user;

    private String documentType;
    
    // Keep the existing database column name for compatibility with current data.
    // New values are Azure Blob object keys, never local filesystem paths.
    @Column(name = "file_path", nullable = false, length = 512)
    private String storageKey;

    private String fileName;

    private String contentType; // image/jpeg

    private Long fileSize;
    
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    private KycStatus status;

    private LocalDateTime submittedAt = LocalDateTime.now();
    
    private LocalDateTime reviewedAt;
}
