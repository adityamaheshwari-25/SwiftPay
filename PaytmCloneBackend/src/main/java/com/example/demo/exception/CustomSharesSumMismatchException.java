package com.example.demo.exception;

import lombok.Getter;

@Getter
public class CustomSharesSumMismatchException extends RuntimeException {
  private final ErrorMessage errorMessage;

  public CustomSharesSumMismatchException(ErrorMessage errorMessage) {
    super(errorMessage.name());
    this.errorMessage = errorMessage;
  }
}
