package com.example.demo.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.repository.IdempotencyKeyRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IdempotencyCleanupJob {
	
	private final IdempotencyKeyRepository idempotencyKeyRepository;
	
	// runs every hour
	@Scheduled(cron = "0 0 * * * ?")
	@Transactional
	public void cleanExpiredKeys() {
		idempotencyKeyRepository.deleteByExpiresAtBefore(LocalDateTime.now());
	}
	
}
