package com.hedgecourt.auth.api.controller;

import com.hedgecourt.auth.api.dto.LoginRequestDto;
import com.hedgecourt.auth.api.dto.LoginResponseDto;
import com.hedgecourt.auth.api.model.User;
import com.hedgecourt.auth.api.service.AuthService;
import com.hedgecourt.spring.lib.annotation.HcPublicEndpoint;
import com.hedgecourt.spring.lib.dto.JwksDto;
import com.hedgecourt.spring.lib.service.HcJwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("")
public class AuthController {
  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthService authService;
  private final HcJwtService jwtService;

  public AuthController(AuthService authService, HcJwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @HcPublicEndpoint
  @PostMapping("/login")
  public LoginResponseDto login(@RequestBody LoginRequestDto request) {
    if (log.isDebugEnabled()) log.debug("login attempt, username=[{}]", request.getUsername());

    User authenticatedUser = authService.authenticate(request);

    return LoginResponseDto.builder().token(jwtService.generateToken(authenticatedUser)).build();
  }

  @HcPublicEndpoint
  @GetMapping("/.well-known/jwks.json")
  public JwksDto getWellKnownJwksJson() {
    if (log.isDebugEnabled()) log.debug("getWellKnownJwksJson()");

    return jwtService.getJwks();
  }

  @HcPublicEndpoint
  @GetMapping("/.well-known/public.pem")
  public String getWellKnownPublicPem() {
    if (log.isDebugEnabled()) log.debug("getWellKnownPublicPem()");
    return jwtService.getPublicKeyPem();
  }
}
