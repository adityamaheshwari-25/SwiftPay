package com.example.demo.exception;

import lombok.Data;

@Data
public class SelfTransferNotAllowedException extends Exception{
	private final ErrorMessage errorMessage;
	
	public SelfTransferNotAllowedException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
