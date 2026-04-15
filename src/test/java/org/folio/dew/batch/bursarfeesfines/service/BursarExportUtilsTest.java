package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;

import java.util.regex.Pattern;
import org.folio.dew.domain.dto.BursarExportJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class BursarExportUtilsTest {

  @Test
  void testGetFilename() {
    String regex = "^lib_\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z\\.dat$";
    Pattern pattern = Pattern.compile(regex);
    assertThat(BursarExportUtils.getFilename(), matchesPattern(pattern));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = { false })
  void testGetJobDescriptionWithoutDryRun(Boolean dryRun) {
    String expected = "# of accounts: 123";
    assertThat(BursarExportUtils.getJobDescription(new BursarExportJob().dryRun(dryRun), 123L), is(expected));
  }

  @Test
  void testGetJobDescriptionWithDryRun() {
    String expected = "[TESTING MODE] # of accounts: 123";
    assertThat(BursarExportUtils.getJobDescription(new BursarExportJob().dryRun(true), 123L), is(expected));
  }
}
