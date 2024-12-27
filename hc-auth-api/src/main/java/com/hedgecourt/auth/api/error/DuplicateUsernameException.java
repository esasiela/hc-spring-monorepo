package com.hedgecourt.auth.api.error;

public class DuplicateUsernameException extends RuntimeException {

  public DuplicateUsernameException(String username) {
    super("Username '" + username + "' already exists.");
  }
}
