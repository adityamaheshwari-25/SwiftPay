package com.example.demo.exception;

import lombok.Data;

@Data
public class SenderWalletNotFoundException extends Exception{
	private final ErrorMessage errorMessage;
	
	public SenderWalletNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
