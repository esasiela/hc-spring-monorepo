package com.hedgecourt.auth.api.controller;

import com.hedgecourt.auth.api.dto.LoginRequestDto;
import com.hedgecourt.auth.api.dto.LoginResponseDto;
import com.hedgecourt.auth.api.model.User;
import com.hedgecourt.auth.api.service.AuthService;
import com.hedgecourt.auth.api.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @PostMapping("")
  public LoginResponseDto login(@RequestBody LoginRequestDto request) {
    if (log.isDebugEnabled()) log.debug("login attempt, username=[{}]", request.getUsername());

    User authenticatedUser = authService.authenticate(request);

    return LoginResponseDto.builder().token(jwtService.generateToken(authenticatedUser)).build();
  }
}