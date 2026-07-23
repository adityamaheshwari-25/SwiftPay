package com.example.demo.audit;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AuditContextBuilder {
	
	public AuditContext fromRequest(HttpServletRequest request) {
		return new AuditContext(
				request.getRequestURI(), 
				request.getMethod(), 
				request.getRemoteAddr()
		);
	}
}
