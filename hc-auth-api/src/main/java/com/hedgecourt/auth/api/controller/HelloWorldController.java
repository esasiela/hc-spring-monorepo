package com.hedgecourt.auth.api.controller;

import com.hedgecourt.spring.lib.HelloWorld;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

  @GetMapping("/helloworld")
  public String helloWorld() {
    return HelloWorld.sayHello();
  }
}
