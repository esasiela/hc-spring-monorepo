package com.hedgecourt.auth;

import com.hedgecourt.spring.HelloWorld;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

  @GetMapping("/helloworld")
  public String helloWorld() {
    return HelloWorld.sayHello();
  }
}
