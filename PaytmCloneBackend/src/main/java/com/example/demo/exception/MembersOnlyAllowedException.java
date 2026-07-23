package com.example.demo.exception;

import lombok.Data;

@Data
public class MembersOnlyAllowedException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public MembersOnlyAllowedException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
