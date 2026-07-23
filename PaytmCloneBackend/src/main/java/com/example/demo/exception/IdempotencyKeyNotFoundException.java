package com.example.demo.exception;

import lombok.Data;

@Data
public class IdempotencyKeyNotFoundException extends Exception{
	private final ErrorMessage errorMessage;
	
	public IdempotencyKeyNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
