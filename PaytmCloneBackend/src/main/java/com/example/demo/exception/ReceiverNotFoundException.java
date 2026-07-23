package com.example.demo.exception;

import lombok.Data;

@Data
public class ReceiverNotFoundException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public ReceiverNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
