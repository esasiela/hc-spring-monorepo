package com.hedgecourt.auth.api.controller;

import com.hedgecourt.auth.api.dto.UserCreateDto;
import com.hedgecourt.auth.api.dto.UserUpdateDto;
import com.hedgecourt.auth.api.service.UserService;
import com.hedgecourt.spring.lib.dto.UserDto;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

  private static final Logger log = LoggerFactory.getLogger(UserController.class);

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  // Aggregate root
  // tag::get-aggregate-root[]
  @GetMapping("")
  @Secured("user:read")
  List<UserDto> list() {
    return userService.list();
  }

  // end::get-aggregate-root[]

  @PostMapping("")
  @Secured("user:write")
  ResponseEntity<UserDto> create(@Valid @RequestBody UserCreateDto userDto) {
    UserDto createdUser = userService.create(userDto);
    return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
  }

  @GetMapping("/{username}")
  @Secured("user:read")
  UserDto retrieve(@PathVariable String username) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (log.isDebugEnabled()) log.debug("authUser Authorities: {}", auth.getAuthorities());

    return userService.retrieve(username);
  }

  @PutMapping("/{username}")
  @Secured("user:write")
  UserDto update(@Valid @RequestBody UserUpdateDto userDto, @PathVariable String username) {
    return userService.update(username, userDto);
  }

  @DeleteMapping("/{username}")
  @Secured("user:write")
  UserDto delete(@PathVariable String username) {
    return userService.delete(username);
  }
}
