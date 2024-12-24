package com.hedgecourt.spring.lib.dto;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {
  private String username;
  private String firstname;
  private String lastname;
  private String email;
  private Set<String> scopes;
}
