package com.example.demo.exception;

import lombok.Getter;

@Getter
public class InvalidCustomShareException extends RuntimeException {
  private final ErrorMessage errorMessage;

  public InvalidCustomShareException(ErrorMessage errorMessage) {
    super(errorMessage.name());
    this.errorMessage = errorMessage;
  }
}

