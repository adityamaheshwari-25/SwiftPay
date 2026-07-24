package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Component
@Validated
@Getter
@Setter
@ConfigurationProperties(prefix = "app.storage.blob")
public class BlobStorageProperties {

	@NotBlank
	private String endpoint;

	@NotBlank
	private String container;
}
