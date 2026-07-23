package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import lombok.Data;


@Entity
@Table(name = "bank_accounts")
@Data
public class BankAccount {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "bankaccount_id")
    private Long id;
	
	// BankAccount
	@ManyToOne
	@JoinColumn(name = "fk_user_id", nullable = false)
	private AppUser user;
	
	private String bankName;
	
	@Column(nullable = false)
    private String accountNumber;
	
	private String ifsc;

	@Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance;
	
	private LocalDateTime createdAt = LocalDateTime.now();
	
	// just for the dummy thing and best practise.
	private boolean verified = false;
	
	@Column(nullable = false)
	private boolean active = true;
	
	@Column(nullable = false)
    private boolean isPrimary = false;
	
	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
		
		
		this.verified = true;
		
		if (this.balance == null) {
			long randomAmount = ThreadLocalRandom.current()
					.nextLong(100_000, 500_001);
			
			this.balance = BigDecimal.valueOf(randomAmount);
		}
	}
	
}
