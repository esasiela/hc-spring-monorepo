package com.hedgecourt.spring.lib.model;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HcUserDetails implements UserDetails {

  private String username;
  private Set<String> scopes;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return scopes.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
  }

  /**
   * Returns an empty password because the core HC Spring libs have no business with the password.
   * Passwords are reserved for hc-auth-api.
   *
   * @return empty string
   */
  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return username;
  }
}
