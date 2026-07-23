package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.example.demo.entity.enums.SplitStatus;
import com.example.demo.entity.enums.SplitType;
import com.example.demo.util.IdGenerator;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "split_requests")
@Data
public class SplitRequest {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "split_request_id")
	private Long id;
	
	@Column(name = "split_code", nullable = false, unique = true, length = 64)
	private String splitCode;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "initiator_user_id", nullable = false)
	private AppUser initiator;
	
	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal totalAmount;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private SplitType splitType = SplitType.EQUAL;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
    private SplitStatus status = SplitStatus.OPEN;
	
	@Column(length = 255)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "splitRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SplitParticipant> participants = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (splitCode == null) splitCode = IdGenerator.generateSplitId();
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
	
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
	
}
