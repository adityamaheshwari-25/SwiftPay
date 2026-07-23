package com.example.demo.service.impl;

import org.springframework.stereotype.Service;

import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditContext;
import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.service.AuditLogService;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Service responsible for persisting audit trail records.
 * <p>
 * Audit logs are immutable system records used for compliance,
 * security investigations, and operational monitoring.
 * <p>
 * This service is intentionally isolated from core business logic
 * so that audit events are recorded even when business transactions fail.
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    
    /**
     * Records a successful business action in the audit log.
     * <p>
     * This method participates in the caller's transaction.
     * If the main transaction is rolled back, the success audit log
     * is also rolled back — which is acceptable because the action
     * ultimately did not succeed.
     *
     * @param userId  ID of the user who performed the action
     * @param role    role of the actor at the time of action (USER, MERCHANT, ADMIN)
     * @param action  audited business action
     * @param context request metadata such as endpoint, HTTP method, and IP address
     */
    @Override
    public void logSuccess(
            Long userId,
            String role,
            AuditAction action,
            AuditContext context
    ) {
        AuditLog log = buildBaseLog(userId, role, action, context);
        log.setOutcome("SUCCESS");
        auditLogRepository.save(log);
    }
    
    /*
     * Why use REQUIRES_NEW for Audit Logs?
In a fintech application, business logic (like a money transfer) and auditing are often handled differently regarding database transactions:

Default (REQUIRED): If the money transfer fails, everything (including the log) is rolled back. You lose the record of the attempt.

REQUIRES_NEW: Spring suspends the "Money Transfer" transaction, opens a completely new database connection for the "Audit Log," commits it, and then resumes the main transaction.

This ensures that even if the transfer fails due to a network error or insufficient funds, your audit table still has a record of the failure.
     * */
    
    /**
     * Records a failed business action in the audit log.
     * <p>
     * This method runs in a {@code REQUIRES_NEW} transaction to guarantee
     * that audit logs are persisted even if the main business transaction fails.
     * <p>
     * This is critical for fintech systems where regulatory compliance
     * requires tracking failed attempts such as:
     * <ul>
     *   <li>Failed money transfers</li>
     *   <li>Invalid MPIN attempts</li>
     *   <li>Unauthorized access attempts</li>
     * </ul>
     *
     * @param userId        ID of the user who attempted the action
     * @param role          role of the actor at the time of action
     * @param action        audited business action
     * @param failureReason human-readable failure reason
     * @param context       request metadata such as endpoint, HTTP method, and IP address
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Critical: Independent of the main TX
    public void logFailure(
            Long userId,
            String role,
            AuditAction action,
            String failureReason,
            AuditContext context
    ) {
        AuditLog log = buildBaseLog(userId, role, action, context);
        log.setOutcome("FAILURE");
        log.setFailureReason(failureReason);
        auditLogRepository.save(log);
    }

    
    /**
     * Builds the common portion of an audit log entry.
     * <p>
     * Centralizing this logic ensures consistency across all audit records
     * and prevents duplication of metadata mapping.
     *
     * @param userId  ID of the acting user
     * @param role    role of the actor
     * @param action  audited business action
     * @param context request metadata captured at runtime
     * @return partially populated {@link AuditLog} entity
     */
    private AuditLog buildBaseLog(
            Long userId,
            String role,
            AuditAction action,
            AuditContext context
    ) {
        AuditLog log = new AuditLog();
        log.setActorUserId(userId);
        log.setActorRole(role);
        log.setAction(action.name());
        log.setEndpoint(context.getEndpoint());
        log.setHttpMethod(context.getHttpMethod());
        log.setIpAddress(context.getIpAddress());
        return log; 
    }
}
