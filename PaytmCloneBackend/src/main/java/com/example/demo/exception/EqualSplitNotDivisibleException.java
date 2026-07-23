package com.example.demo.exception;

import lombok.Getter;

@Getter
public class EqualSplitNotDivisibleException extends RuntimeException {
  private final ErrorMessage errorMessage;

  public EqualSplitNotDivisibleException(ErrorMessage errorMessage) {
    super(errorMessage.name());
    this.errorMessage = errorMessage;
  }
}

