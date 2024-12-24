package com.hedgecourt.spring.lib.error;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class BuildInfoAdvice {
  @ExceptionHandler(BuildInfoException.class)
  public ResponseEntity<Map<String, Object>> handleBuildInfoException(BuildInfoException ex) {
    Map<String, Object> response = new HashMap<>();
    response.put("error", ex.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}
