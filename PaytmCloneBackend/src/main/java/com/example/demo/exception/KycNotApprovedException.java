package com.example.demo.exception;

import lombok.Data;

@Data
public class KycNotApprovedException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public KycNotApprovedException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
