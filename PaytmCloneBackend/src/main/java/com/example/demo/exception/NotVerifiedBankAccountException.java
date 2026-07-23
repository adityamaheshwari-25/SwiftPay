package com.example.demo.exception;

import lombok.Data;

@Data
public class NotVerifiedBankAccountException extends Exception{

	private final ErrorMessage errorMessage;
	
	public NotVerifiedBankAccountException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
