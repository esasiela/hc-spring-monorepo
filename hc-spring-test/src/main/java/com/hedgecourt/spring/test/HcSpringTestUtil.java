package com.hedgecourt.spring.test;

import org.junit.jupiter.api.Assertions;

/**
 * A helper class mostly here to help the author understand the difference between what goes in
 * "src/main" and "src/test" in a project that packages up tests intended for "scope=test"
 * dependencies.
 */
public final class HcSpringTestUtil {
  private HcSpringTestUtil() {}

  public static void assertHcEqualStrings(String expected, String actual) {
    Assertions.assertEquals(expected, actual, "HC Strings are not equal");
  }
}
