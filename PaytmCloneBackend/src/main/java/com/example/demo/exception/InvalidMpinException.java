package com.example.demo.exception;

import lombok.Data;

@Data
public class InvalidMpinException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public InvalidMpinException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
