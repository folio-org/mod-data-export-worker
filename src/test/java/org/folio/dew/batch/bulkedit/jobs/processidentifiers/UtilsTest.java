package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class UtilsTest {

  @Test
  void testEncode() {
    var barcode = "bar*code";
    var actual = Utils.encode(barcode);
    assertEquals("\"bar\\*code\"", actual);
  }
}
