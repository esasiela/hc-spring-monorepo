package com.hedgecourt.auth.api.controller;

import com.hedgecourt.auth.api.dto.ScopeCreateDto;
import com.hedgecourt.auth.api.model.Scope;
import com.hedgecourt.auth.api.service.ScopeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
  @Secured("SCOPE_admin:read")
  List<Scope> list() {
    return scopeService.list();
  }

  // end::get-aggregate-root[]

  @PostMapping("")
  @Secured("SCOPE_admin:write")
  ResponseEntity<List<Scope>> createBulk(
      Authentication auth, @Valid @RequestBody List<ScopeCreateDto> scopeDtos) {
    if (log.isInfoEnabled()) log.info("Processing scope bulk create, authUser={}", auth.getName());

    List<Scope> scopes = scopeService.createBulk(scopeDtos);
    return ResponseEntity.ok(scopes);
  }
}
