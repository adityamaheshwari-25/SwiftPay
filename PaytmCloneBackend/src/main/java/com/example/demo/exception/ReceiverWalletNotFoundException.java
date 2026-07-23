package com.example.demo.exception;

import lombok.Data;

@Data
public class ReceiverWalletNotFoundException extends Exception{
	private final ErrorMessage errorMessage;
	
	public ReceiverWalletNotFoundException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
