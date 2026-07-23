package com.example.demo.audit;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuditContext {
	private String endpoint;
	private String httpMethod;
	private String ipAddress;
}
