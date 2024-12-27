package com.hedgecourt.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtAuthenticationErrorResponseDto {
  private String error;
  private String message;

  public JwtAuthenticationErrorResponseDto(String message) {
    this.error = "Unauthorized";
    this.message = message;
  }
}
