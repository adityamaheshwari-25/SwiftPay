package com.example.demo.aspect;

import com.example.demo.audit.AuditAction;
import com.example.demo.audit.AuditContext;
import com.example.demo.audit.AuditContextBuilder;
import com.example.demo.entity.AppUser;
import com.example.demo.security.AppUserDetails;
import com.example.demo.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;
    private final AuditContextBuilder contextBuilder;
    private final HttpServletRequest request;

    // Matches all methods in your service implementation package
    @Pointcut("execution(* com.example.demo.service.impl.*ServiceImpl.*(..))")
    public void serviceMethods() {}

    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logSuccess(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, "SUCCESS", null);
    }

    @AfterThrowing(pointcut = "serviceMethods()", throwing = "ex")
    public void logFailure(JoinPoint joinPoint, Exception ex) {
        handleLog(joinPoint, "FAILURE", ex.getMessage());
    }

    private void handleLog(JoinPoint joinPoint, String outcome, String reason) {
        String methodName = joinPoint.getSignature().getName();
        AuditAction action = mapMethodToAction(methodName);
        
        if (action == null) return; 

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String role = "GUEST";

        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            Object principal = auth.getPrincipal();
            
            // FIX: In your security config, the principal is AppUserDetails
            if (principal instanceof AppUserDetails userDetails) {
                // We need to access the private 'user' field. 
                // Since AppUserDetails doesn't have a getter for user, 
                // ensure you add @Getter to AppUserDetails or a getUser() method.
                AppUser user = userDetails.getUser(); 
                userId = user.getId();
                role = user.getRole().name();
            } 
            // Fallback if it's during login or registration where Principal might be different
            else if (principal instanceof AppUser user) {
                userId = user.getId();
                role = user.getRole().name();
            }
        }

        AuditContext context = contextBuilder.fromRequest(request);

        if ("SUCCESS".equals(outcome)) {
            auditLogService.logSuccess(userId, role, action, context);
        } else {
            auditLogService.logFailure(userId, role, action, reason, context);
        }
    }

    private AuditAction mapMethodToAction(String methodName) {
        return switch (methodName) {
            case "login" -> AuditAction.LOGIN;
            case "registerUser" -> AuditAction.REGISTER_USER;
            case "registerMerchant" -> AuditAction.REGISTER_MERCHANT;
            case "addMoney" -> AuditAction.ADD_MONEY;
            case "withdrawMoney" -> AuditAction.WITHDRAW;
            case "transferWalletToWallet" -> AuditAction.WALLET_TRANSFER;
            case "viewWallet" -> AuditAction.VIEW_WALLET;
            case "createBankAccount" -> AuditAction.ADD_BANK_ACCOUNT;
            case "getMyBankAccounts" -> AuditAction.VIEW_BANK_ACCOUNTS;
            case "uploadKyc" -> AuditAction.UPLOAD_KYC;
            case "approveKyc" -> AuditAction.APPROVE_KYC;
            case "rejectKyc" -> AuditAction.REJECT_KYC;
            case "setMpin" -> AuditAction.SET_MPIN;
            case "verifyMpin" -> AuditAction.VERIFY_MPIN;
            case "lookupByMobile" -> AuditAction.LOOKUP_USER;
            case "getMyTransactions", "getMerchantTransactions" -> AuditAction.VIEW_TRANSACTIONS;
            default -> null; 
        };
    }
}