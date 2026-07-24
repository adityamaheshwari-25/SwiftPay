package com.example.demo.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

	@NotBlank
	@Size(min = 32)
	private String secret;

	@NotNull
	private Duration expiration = Duration.ofHours(10);
}
