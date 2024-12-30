package com.hedgecourt.auth.api;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.hedgecourt.spring.lib.service.HcJwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class HcAuthApiApplicationTest {
  @Autowired HcJwtService jwtService;

  @Test
  void contextLoads() {
    assertNotNull(jwtService);
  }
}
