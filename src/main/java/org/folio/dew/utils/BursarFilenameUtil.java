package org.folio.dew.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class BursarFilenameUtil {

  public static final String CHARGE_FEESFINES_EXPORT_STEP = "CHARGE_FEESFINES_EXPORT_STEP";
  public static final String REFUND_FEESFINES_EXPORT_STEP = "REFUND_FEESFINES_EXPORT_STEP";

  private static final String CHARGE_FILE_NAME_PATTERN = "lib_%sa.dat";
  private static final String REFUND_FILE_NAME_PATTERN = "lib_%sb.dat";
  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

  public static String getFilename(String stepName) {
    String fileNamePattern;
    if (CHARGE_FEESFINES_EXPORT_STEP.equals(stepName)) {
      fileNamePattern = CHARGE_FILE_NAME_PATTERN;
    } else if (REFUND_FEESFINES_EXPORT_STEP.equals(stepName)) {
      fileNamePattern = REFUND_FILE_NAME_PATTERN;
    } else {
      throw new IllegalArgumentException(String.format("Can't create file name for step %s", stepName));
    }

    return String.format(fileNamePattern, LocalDateTime.now().format(DATE_TIME_FORMATTER));
  }

}
