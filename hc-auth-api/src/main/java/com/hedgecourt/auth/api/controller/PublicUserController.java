package com.hedgecourt.auth.api.controller;

import com.hedgecourt.auth.api.service.UserService;
import com.hedgecourt.spring.lib.annotation.HcPublicEndpoint;
import com.hedgecourt.spring.lib.dto.UserDto;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/publicUsers")
public class PublicUserController {

  private static final Logger log = LoggerFactory.getLogger(PublicUserController.class);

  private final UserService userService;

  PublicUserController(UserService userService) {
    this.userService = userService;
  }

  // Aggregate root
  // tag::get-aggregate-root[]
  @GetMapping("")
  @HcPublicEndpoint
  List<UserDto> listPublicUsers() {
    return userService.listByScopeName("user:public");
  }

  // end::get-aggregate-root[]

}
