package org.folio.dew.batch.bursarfeesfines.service;

import java.time.Instant;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BursarFeesFinesUtils {

  public static final String EXPORT_STEP = "EXPORT_STEP";

  private static final String FILE_PATTERN = "lib_%s.dat";
  private static final String DESCRIPTION_PATTERN = "# of accounts: %d";

  public static String getFilename() {
    return String.format(FILE_PATTERN, Instant.now().toString());
  }

  public static String getJobDescriptionPart() {
    return DESCRIPTION_PATTERN;
  }
}
