package com.example.demo.exception;

import lombok.Data;

@Data
public class UserNotFoundException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public UserNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
