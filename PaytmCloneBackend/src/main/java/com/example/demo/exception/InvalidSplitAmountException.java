package com.example.demo.exception;

import lombok.Getter;

@Getter
public class InvalidSplitAmountException extends RuntimeException {
  private final ErrorMessage errorMessage;

  public InvalidSplitAmountException(ErrorMessage errorMessage) {
    super(errorMessage.name());
    this.errorMessage = errorMessage;
  }
}
