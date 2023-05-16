package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class BursarExportUtilsTest {

  @Test
  void testGetFilename() {
    String regex =
      "^lib_\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z\\.dat$";
    Pattern pattern = Pattern.compile(regex);
    assertThat(BursarExportUtils.getFilename(), matchesPattern(pattern));
  }

  @Test
  void testGetJobDescriptionPart() {
    String expected = "# of accounts: %d";
    assertThat(BursarExportUtils.getJobDescriptionPart(), is(expected));
  }
}
