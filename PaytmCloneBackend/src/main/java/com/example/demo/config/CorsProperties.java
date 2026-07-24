package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

	@NotEmpty
	private List<@NotBlank String> allowedOrigins = new ArrayList<>();

	@AssertTrue(message = "CORS origins must be explicit; wildcard origins are not allowed")
	public boolean isWildcardFree() {
		return allowedOrigins.stream().noneMatch(origin -> origin.contains("*"));
	}
}
