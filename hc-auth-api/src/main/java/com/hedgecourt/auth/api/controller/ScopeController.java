package com.hedgecourt.auth.api.controller;

import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.service.ScopeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/scopes")
@SecurityRequirement(name = "bearerAuth")
public class ScopeController {
  private static final Logger log = LoggerFactory.getLogger(ScopeController.class);

  private final ScopeService scopeService;

  ScopeController(ScopeService scopeService) {
    this.scopeService = scopeService;
  }

  // Aggregate root
  // tag::get-aggregate-root[]
  @GetMapping("")
  // TODO include @Secured("[REQUIRED_SCOPE]")
  List<Scope> list() {
    return scopeService.list();
  }
}
