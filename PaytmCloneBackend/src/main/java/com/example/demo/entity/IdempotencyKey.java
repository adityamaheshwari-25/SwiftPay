package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
		name = "idempotency_keys",
		uniqueConstraints = @UniqueConstraint(
					columnNames = {"user_id", "api_name", "idempotency_key"}
				)
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private Long userId;
	
	private String apiName;
	
	private String idempotencyKey;
	
	@Column(columnDefinition = "TEXT")
	private String requestHash;
	
	@Column(columnDefinition = "TEXT")
	private String responseBody;
	
	private LocalDateTime createdAt;
	
	// for TTL SUPPORT
    private LocalDateTime expiresAt;

}
