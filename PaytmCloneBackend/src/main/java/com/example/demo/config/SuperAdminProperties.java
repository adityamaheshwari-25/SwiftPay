package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "app.superadmin")
@Data
public class SuperAdminProperties {
	private String email;
	private String password;
}
