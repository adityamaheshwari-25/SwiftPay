package com.example.demo.exception;

import lombok.Getter;

@Getter
public class SplitBadRequestException extends RuntimeException {
  private final ErrorMessage errorMessage;

  public SplitBadRequestException(ErrorMessage errorMessage) {
    super(errorMessage.name());
    this.errorMessage = errorMessage;
  }
}