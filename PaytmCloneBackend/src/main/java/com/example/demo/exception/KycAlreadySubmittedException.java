package com.example.demo.exception;

import lombok.Data;

@Data
public class KycAlreadySubmittedException extends Exception{
	private final ErrorMessage errorMessage;
	
	public KycAlreadySubmittedException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
