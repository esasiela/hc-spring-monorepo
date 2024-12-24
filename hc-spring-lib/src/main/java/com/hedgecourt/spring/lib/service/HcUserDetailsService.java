package com.hedgecourt.spring.lib.service;

import com.hedgecourt.spring.lib.dto.UserDto;
import com.hedgecourt.spring.lib.error.UserNotFoundException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * This class is for HedgeCourt Plugins to interact with the HC Auth API to get user information.
 * This class also serves as the Spring Boot UserDetailsService for plugins, loading user data via
 * HTTP requests to the HC Auth API.
 */
@Service
public class HcUserDetailsService implements UserDetailsService {

  private static final Logger log = LoggerFactory.getLogger(HcUserDetailsService.class);

  public UserDto retrieve(String username) throws UserNotFoundException {
    // TODO retrieve the user from hc-auth-api via HTTP

    if ("notfound".equals(username)) {
      throw new UserNotFoundException(username);
    }

    return UserDto.builder()
        .username(username)
        .firstname("Bilbo")
        .lastname("Baggins")
        .email("mrunderhill@shire.net")
        .scopes(Set.of("user:read", "dev:read"))
        .build();
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    log.debug("loadUserByUsername({})", username);
    UserDto userDto = null;
    try {
      userDto = retrieve(username);
    } catch (UserNotFoundException ex) {
      throw new UsernameNotFoundException("User not found: " + username, ex);
    }

    return new HcUserDetails(userDto);
  }
}

class HcUserDetails implements UserDetails {

  UserDto userDto;

  public HcUserDetails(UserDto userDto) {
    this.userDto = userDto;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return userDto.getScopes().stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toSet());
  }

  @Override
  public String getPassword() {
    // TODO figure out wtf to do with credentials in a HC plugin
    return "";
  }

  @Override
  public String getUsername() {
    return userDto.getUsername();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
