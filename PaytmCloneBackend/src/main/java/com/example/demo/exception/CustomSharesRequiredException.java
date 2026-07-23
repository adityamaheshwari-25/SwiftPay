package com.example.demo.exception;

import lombok.Getter;

@Getter
public class CustomSharesRequiredException extends RuntimeException {
  private final ErrorMessage errorMessage;

  public CustomSharesRequiredException(ErrorMessage errorMessage) {
    super(errorMessage.name());
    this.errorMessage = errorMessage;
  }
}
