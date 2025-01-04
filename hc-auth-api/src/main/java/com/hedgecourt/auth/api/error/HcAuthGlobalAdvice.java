package com.hedgecourt.auth.api.error;

import com.hedgecourt.spring.lib.error.UserNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class HcAuthGlobalAdvice {

  @ExceptionHandler(DuplicateUsernameException.class)
  public ResponseEntity<Map<String, String>> duplicateUsernameHandler(
      DuplicateUsernameException ex) {
    Map<String, String> errors = new HashMap<>();
    errors.put("message", ex.getMessage());
    return new ResponseEntity<>(errors, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<Map<String, String>> userNotFoundHandler(UserNotFoundException ex) {
    Map<String, String> errors = new HashMap<>();
    errors.put("message", ex.getMessage());
    return new ResponseEntity<>(errors, HttpStatus.NOT_FOUND);
  }
}
