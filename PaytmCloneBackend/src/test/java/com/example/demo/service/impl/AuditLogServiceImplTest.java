package com.example.demo.service.impl;


import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditContext;
import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 *How do you test this REQUIRES_NEW behavior?
 *Unit tests with Mockito verify mapping and repository interaction. Transaction propagation is a 
 *Spring/DB concern, so I validate it with an integration test using @SpringBootTest + real database 
 *(or Testcontainers) to confirm audit rows commit even when the business transaction rolls back. 
 */

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @Test
    void logSuccess_shouldSaveAuditLog_withSuccessOutcome_andMappedFields() {
        // Arrange
        Long userId = 10L;
        String role = "USER";
        AuditAction action = AuditAction.LOGIN; // change to an action that exists in your enum

        AuditContext context = mock(AuditContext.class);
        when(context.getEndpoint()).thenReturn("/api/auth/login");
        when(context.getHttpMethod()).thenReturn("POST");
        when(context.getIpAddress()).thenReturn("127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // Act
        auditLogService.logSuccess(userId, role, action, context);

        // Assert
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(userId, saved.getActorUserId());
        assertEquals(role, saved.getActorRole());
        assertEquals(action.name(), saved.getAction());

        assertEquals("/api/auth/login", saved.getEndpoint());
        assertEquals("POST", saved.getHttpMethod());
        assertEquals("127.0.0.1", saved.getIpAddress());

        assertEquals("SUCCESS", saved.getOutcome());
        assertNull(saved.getFailureReason()); // should not be set for success
        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    void logFailure_shouldSaveAuditLog_withFailureOutcome_failureReason_andMappedFields() {
        // Arrange
        Long userId = 20L;
        String role = "MERCHANT";
        AuditAction action = AuditAction.WALLET_TRANSFER; // change to an action that exists in your enum
        String failureReason = "Insufficient balance";

        AuditContext context = mock(AuditContext.class);
        when(context.getEndpoint()).thenReturn("/api/transfer");
        when(context.getHttpMethod()).thenReturn("POST");
        when(context.getIpAddress()).thenReturn("10.0.0.5");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        // Act
        auditLogService.logFailure(userId, role, action, failureReason, context);

        // Assert
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals(userId, saved.getActorUserId());
        assertEquals(role, saved.getActorRole());
        assertEquals(action.name(), saved.getAction());

        assertEquals("/api/transfer", saved.getEndpoint());
        assertEquals("POST", saved.getHttpMethod());
        assertEquals("10.0.0.5", saved.getIpAddress());

        assertEquals("FAILURE", saved.getOutcome());
        assertEquals("Insufficient balance", saved.getFailureReason());
        verifyNoMoreInteractions(auditLogRepository);
    }
}
