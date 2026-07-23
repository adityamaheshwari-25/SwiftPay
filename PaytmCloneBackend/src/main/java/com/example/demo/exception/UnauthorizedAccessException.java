package com.example.demo.exception;

import lombok.Data;

@Data
public class UnauthorizedAccessException extends Exception{
	private final ErrorMessage errorMessage;
	
	public UnauthorizedAccessException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
