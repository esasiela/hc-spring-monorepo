package com.hedgecourt.spring.test;

import org.junit.jupiter.api.Test;

/**
 * A helper class mostly here to help the author understand the difference between what goes in
 * "src/main" and "src/test" in a project that packages up tests intended for "scope=test"
 * dependencies.
 */
public class HcSpringTestUtilTest {

  @Test
  void testAssertHcEqualStrings() {
    HcSpringTestUtil.assertHcEqualStrings("Hello", "Hello");
  }
}
