package com.example.demo.exception;

import lombok.Data;

@Data
public class UserAlreadyExistsException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public UserAlreadyExistsException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
