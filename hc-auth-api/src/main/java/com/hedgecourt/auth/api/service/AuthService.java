package com.hedgecourt.auth.api.service;

import com.hedgecourt.auth.api.dto.LoginRequestDto;
import com.hedgecourt.auth.api.model.User;
import com.hedgecourt.auth.api.model.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  private final UserRepository userRepository;
  private final AuthenticationManager authenticationManager;

  public AuthService(UserRepository userRepository, AuthenticationManager authenticationManager) {
    this.userRepository = userRepository;
    this.authenticationManager = authenticationManager;
  }

  public User authenticate(LoginRequestDto request) {
    if (log.isDebugEnabled())
      log.debug("authenticate attempt, username=[{}]", request.getUsername());

    Authentication a =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

    // TODO return the Authentication object instead of a User object
    if (log.isDebugEnabled())
      log.debug("authenticate [{}] [{}]", a.getPrincipal(), a.isAuthenticated());

    return userRepository
        .findById(request.getUsername())
        .orElseThrow(() -> new BadCredentialsException(request.getUsername()));
  }
}
