package org.folio.dew.service;


import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpecialCharacterEscaperTest {

  @Test
  void escapeTest() {
    var escaper = new SpecialCharacterEscaper();
    var expected = "test%3Btest";
    var actual = escaper.escape("test;test");
    assertEquals(expected, actual);
  }

  @Test
  void escapeIfEmptyTest() {
    var escaper = new SpecialCharacterEscaper();
    var actual = escaper.escape("");
    assertEquals("", actual);
  }

  @Test
  void escapeListTest() {
    var escaper = new SpecialCharacterEscaper();
    var actual = escaper.escape(List.of("test;test", "test|test", "test:test"));
    assertEquals("test%3Btest", actual.get(0));
    assertEquals("test%7Ctest", actual.get(1));
    assertEquals("test%3Atest", actual.get(2));
  }

  @Test
  void escapeListIfNullTest() {
    var escaper = new SpecialCharacterEscaper();
    List<String> arg = null;
    List<String> actual = escaper.escape(arg);
    assertEquals(Collections.emptyList(), actual);
  }

  @Test
  void restoreTest() {
    var escaper = new SpecialCharacterEscaper();
    var expected = "test;test";
    var actual = escaper.restore("test%3Btest");
    assertEquals(expected, actual);
  }

  @Test
  void restoreIfEmptyTest() {
    var escaper = new SpecialCharacterEscaper();
    var actual = escaper.restore("");
    assertEquals("", actual);
  }

  @Test
  void restoreListTest() {
    var escaper = new SpecialCharacterEscaper();
    var actual = escaper.restore(List.of("test%3Btest", "test%7Ctest", "test%3Atest"));
    assertEquals("test;test", actual.get(0));
    assertEquals("test|test", actual.get(1));
    assertEquals("test:test", actual.get(2));
  }


  @Test
  void restoreListIfNullTest() {
    var escaper = new SpecialCharacterEscaper();
    List<String> arg = null;
    List<String> actual = escaper.restore(arg);
    assertEquals(Collections.emptyList(), actual);
  }
}
