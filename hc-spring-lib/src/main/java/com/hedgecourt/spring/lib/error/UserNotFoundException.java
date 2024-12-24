package com.hedgecourt.spring.lib.error;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String username) {
    super("Could not find user: " + username);
  }
}
