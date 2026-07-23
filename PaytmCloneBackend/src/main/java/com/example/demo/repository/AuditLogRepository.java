package com.example.demo.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.AuditLog;

import jakarta.transaction.Transactional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>{
	@Modifying
    @Transactional
    @Query("DELETE FROM AuditLog a WHERE a.createdAt < :cutoffDate")
    int deleteOldLogs(@Param("cutoffDate") LocalDateTime cutoffDate);
}
