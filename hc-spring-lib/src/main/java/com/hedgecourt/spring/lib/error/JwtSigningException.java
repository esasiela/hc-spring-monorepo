package com.hedgecourt.spring.lib.error;

public class JwtSigningException extends RuntimeException {
  public JwtSigningException(String message, Exception cause) {
    super(message, cause);
  }
}
