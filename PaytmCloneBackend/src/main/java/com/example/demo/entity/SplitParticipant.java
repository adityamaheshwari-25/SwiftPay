package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.entity.enums.SplitParticipantState;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Data;

@Entity
@Table(
	name = "split_participants",
	uniqueConstraints = @UniqueConstraint(columnNames = {"split_request_id", "participant_user_id"})
)
@Data
public class SplitParticipant {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "split_participant_id")
	private Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "split_request_id", nullable = false)
	private SplitRequest splitRequest;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_user_id", nullable = false)
    private AppUser participant;
	
	@Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal shareAmount;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SplitParticipantState state = SplitParticipantState.PENDING;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "paid_transaction_id", unique = true)
	private Transaction paidTransaction;
	
	private LocalDateTime paidAt;
	
	@Version
    private Long version; // ✅ optimistic locking
	
}
