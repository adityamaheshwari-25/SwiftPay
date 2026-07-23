package com.example.demo.exception;

import lombok.Data;

@Data
public class MpinNotSetException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public MpinNotSetException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
