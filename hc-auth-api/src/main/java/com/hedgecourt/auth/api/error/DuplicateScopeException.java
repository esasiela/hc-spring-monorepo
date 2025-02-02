package com.hedgecourt.auth.api.error;

public class DuplicateScopeException extends RuntimeException {
  public DuplicateScopeException(String message, Exception e) {
    super(message, e);
  }
}
