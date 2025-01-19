package com.hedgecourt.auth.api.controller;

import com.hedgecourt.auth.api.dto.NavItemDto;
import com.hedgecourt.auth.api.service.NavService;
import com.hedgecourt.spring.lib.annotation.HcPublicEndpoint;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/nav")
public class NavController {
  private static final Logger log = LoggerFactory.getLogger(NavController.class);

  private final NavService navService;

  NavController(NavService navService) {
    this.navService = navService;
  }

  // Aggregate root
  // tag::get-aggregate-root[]
  @GetMapping("")
  @HcPublicEndpoint
  List<NavItemDto> list(Authentication auth) {
    if (log.isDebugEnabled())
      log.debug("navService.list() is auth null: {}", (auth == null ? "yes" : "no"));
    if (log.isDebugEnabled())
      log.debug("navService.list() authUser={}", (auth == null ? "null" : auth.getName()));

    List<NavItemDto> navItems = navService.list();

    // TODO implement anonymous nav list (instead of just top-level navs)
    if (auth == null) {
      if (log.isInfoEnabled()) log.info("null auth, filtering list by childOf==0");
      return navItems.stream()
          .filter(dto -> dto.getChildOf() == null || dto.getChildOf() == 0)
          .collect(Collectors.toList());
    } else {
      return navItems;
    }
  }

  // end::get-aggregate-root[]

  @DeleteMapping("/clobber")
  @Secured("SCOPE_dev:write")
  public ResponseEntity<List<NavItemDto>> bulkDelete(Authentication auth) {
    if (log.isInfoEnabled())
      log.info("Processing nav clobber DELETE request (bulk delete), user={}", auth.getName());
    List<NavItemDto> existingItems = navService.bulkDelete();
    return ResponseEntity.ok(existingItems);
  }

  @PostMapping("/clobber")
  @Secured("SCOPE_dev:write")
  public ResponseEntity<List<NavItemDto>> bulkAdd(
      @RequestBody List<NavItemDto> navItems, Authentication auth) {
    if (log.isInfoEnabled())
      log.info("Processing nav clobber POST request (bulk add), user={}", auth.getName());
    List<NavItemDto> newItems = navService.bulkAdd(navItems, true);
    return ResponseEntity.ok(newItems);
  }
}
