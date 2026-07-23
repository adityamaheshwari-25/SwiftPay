package com.example.demo.exception;

import lombok.Data;

@Data
public class WalletNotFoundException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public WalletNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
