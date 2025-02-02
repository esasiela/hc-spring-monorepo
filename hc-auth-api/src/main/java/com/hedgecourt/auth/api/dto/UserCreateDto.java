package com.hedgecourt.auth.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserCreateDto extends UserUpdateDto {

  @NotNull(message = "Username is required")
  @Size(min = 5, max = 20, message = "Username must be between 6-20 characters long")
  @Pattern(
      regexp = "^[a-zA-Z][a-zA-Z0-9_-]*$",
      message =
          "Username must start with an alphabet and contain only alphanumeric characters, underscores, or hyphens.")
  private String username;

  @NotNull(message = "Password is required")
  @Size(min = 5, max = 32, message = "Password must be between 6-32 characters long")
  @Pattern(
      regexp = "^[a-zA-Z0-9!@#$%^&*();_+=-]+$",
      message =
          "Password can only contain letters, numbers, and the following special characters: !@#$%^&*()_+=-")
  private String plaintextPassword;

  private Set<String> scopes;
}
