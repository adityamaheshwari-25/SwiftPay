package com.example.demo.exception;

import lombok.Data;

@Data
public class LessAmountException extends Exception{
	private final ErrorMessage errorMessage;
	
	public LessAmountException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
