package org.folio.dew.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class BursarFilenameUtil {

  public static final String CHARGE_FEESFINES_EXPORT_STEP = "CHARGE_FEESFINES_EXPORT_STEP";
  public static final String REFUND_FEESFINES_EXPORT_STEP = "REFUND_FEESFINES_EXPORT_STEP";
  private static final String CHARGE_FILE_NAME_PATTERN = "lib_%sa.dat";
  private static final String REFUND_FILE_NAME_PATTERN = "lib_%sb.dat";

  public static String getFilename(String stepName) {
    String fileNamePattern = "";
    if (CHARGE_FEESFINES_EXPORT_STEP.equals(stepName)) {
      fileNamePattern = CHARGE_FILE_NAME_PATTERN;
    } else if (REFUND_FEESFINES_EXPORT_STEP.equals(stepName)) {
      fileNamePattern = REFUND_FILE_NAME_PATTERN;
    }
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd");
    String now = LocalDateTime.now().format(dateTimeFormatter);
    return String.format(fileNamePattern, now);
  }
}
