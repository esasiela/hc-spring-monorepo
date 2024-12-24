package com.hedgecourt.spring.lib.error;

public class BuildInfoException extends RuntimeException {
  public BuildInfoException(Exception ex) {
    super(ex);
  }
}
