package com.hedgecourt.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HelloWorldTest {

  @Test
  void testSayHello() {
    String expected = "Hello, World!";
    String actual = HelloWorld.sayHello();
    assertEquals(expected, actual, "The sayHello method should return the correct greeting");
  }
}
