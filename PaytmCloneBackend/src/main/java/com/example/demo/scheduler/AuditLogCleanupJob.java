package com.example.demo.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.demo.repository.AuditLogRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditLogCleanupJob {
	
	private final AuditLogRepository auditLogRepository;
	
	@Scheduled(cron = "0 0 2 * * *")
    @Transactional // Required because we are deleting
    public void execute() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        auditLogRepository.deleteOldLogs(cutoff);
    }
}
