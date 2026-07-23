package com.example.demo.service;

import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditContext;

public interface AuditLogService {
	void logSuccess(
            Long userId,
            String role,
            AuditAction action,
            AuditContext context
    );

    void logFailure(
            Long userId,
            String role,
            AuditAction action,
            String failureReason,
            AuditContext context
    );
}
