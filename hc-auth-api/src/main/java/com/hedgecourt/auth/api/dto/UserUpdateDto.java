package com.hedgecourt.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserUpdateDto {

  @Size(max = 30, message = "First name cannot be longer than 30 characters")
  private String firstname;

  @Size(max = 30, message = "Last name cannot be longer than 30 characters")
  private String lastname;

  @NotBlank(message = "Email is required")
  @Pattern(
      regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
      message = "Invalid email format")
  private String email;
}
