package org.folio.dew.batch.bursarfeesfines.service;

import java.time.Instant;
import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.BursarExportJob;

@UtilityClass
public class BursarExportUtils {

  public static final String GET_FILENAME_STEP = "GET_FILENAME";
  public static final String EXPORT_STEP = "EXPORT_STEP";

  private static final String FILE_PATTERN = "lib_%s.dat";
  private static final String DESCRIPTION_PATTERN = "# of accounts: %d";
  private static final String DRY_RUN_DESCRIPTION_PATTERN = "[TESTING MODE] # of accounts: %d";

  public static String getFilename() {
    return String.format(FILE_PATTERN, Instant.now().toString());
  }

  public static String getJobDescription(BursarExportJob jobConfig, long numWrites) {
    if (Boolean.TRUE.equals(jobConfig.getDryRun())) {
      return String.format(DRY_RUN_DESCRIPTION_PATTERN, numWrites);
    } else {
      return String.format(DESCRIPTION_PATTERN, numWrites);
    }
  }
}
