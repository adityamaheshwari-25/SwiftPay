package com.example.demo.exception;

import lombok.Data;

@Data
public class IdempotencyKeyConflictException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public IdempotencyKeyConflictException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}	
