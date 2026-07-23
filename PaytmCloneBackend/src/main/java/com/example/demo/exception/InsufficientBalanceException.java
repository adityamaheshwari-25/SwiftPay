package com.example.demo.exception;

import lombok.Data;

@Data // this data annotation is for the getters and setters, basically for getting the error message, that's it.
public class InsufficientBalanceException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public InsufficientBalanceException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
