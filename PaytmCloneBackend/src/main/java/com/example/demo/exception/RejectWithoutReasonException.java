package com.example.demo.exception;

import lombok.Data;

@Data
public class RejectWithoutReasonException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public RejectWithoutReasonException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
