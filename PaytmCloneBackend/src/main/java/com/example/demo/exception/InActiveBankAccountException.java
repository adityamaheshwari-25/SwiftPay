package com.example.demo.exception;

import lombok.Data;

@Data
public class InActiveBankAccountException extends RuntimeException{
private final ErrorMessage errorMessage;
	
	public InActiveBankAccountException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
