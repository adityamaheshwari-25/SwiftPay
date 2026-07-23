package com.example.demo.exception;

import lombok.Data;

@Data
public class MembersAreRequiredException extends RuntimeException{
	private final ErrorMessage errorMessage;
	
	public MembersAreRequiredException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
