package com.example.demo.exception;

import lombok.Data;

@Data
public class LargerFileSizeException extends Exception{
	private final ErrorMessage errorMessage;
	
	public LargerFileSizeException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
