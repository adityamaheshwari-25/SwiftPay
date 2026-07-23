package com.example.demo.exception;

import lombok.Data;

@Data
public class ResourceNotFoundException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public ResourceNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
