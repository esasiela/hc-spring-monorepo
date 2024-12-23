package com.hedgecourt.auth.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.hedgecourt")
public class HcAuthApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(HcAuthApiApplication.class, args);
  }
}
