package com.hedgecourt.auth.api.error;

import java.util.Set;

public class InvalidScopeException extends RuntimeException {

  public InvalidScopeException(String scope) {
    this(Set.of(scope));
  }

  public InvalidScopeException(Set<String> scopes) {
    super("Invalid Scope: " + scopes);
  }
}
