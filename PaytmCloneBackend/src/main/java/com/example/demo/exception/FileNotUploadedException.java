package com.example.demo.exception;

import lombok.Data;

@Data
public class FileNotUploadedException extends Exception{
	private final ErrorMessage errorMessage;
	
	public FileNotUploadedException(ErrorMessage errorMessage) {
		super(errorMessage.name());
		this.errorMessage = errorMessage;
	}
}
