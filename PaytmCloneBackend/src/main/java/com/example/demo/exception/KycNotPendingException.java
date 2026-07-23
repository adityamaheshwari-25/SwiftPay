package com.example.demo.exception;

import lombok.Data;

@Data
public class KycNotPendingException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public KycNotPendingException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
