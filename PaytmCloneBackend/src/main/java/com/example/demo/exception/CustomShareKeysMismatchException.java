package com.example.demo.exception;

import lombok.Getter;

@Getter
public class CustomShareKeysMismatchException extends RuntimeException {
  private final ErrorMessage errorMessage;

  public CustomShareKeysMismatchException(ErrorMessage errorMessage) {
    super(errorMessage.name());
    this.errorMessage = errorMessage;
  }
}

