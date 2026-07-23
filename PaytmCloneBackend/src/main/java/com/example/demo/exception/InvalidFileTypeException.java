package com.example.demo.exception;

import lombok.Data;

@Data
public class InvalidFileTypeException extends Exception{
	private final ErrorMessage errorMessage;
	
	public InvalidFileTypeException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
