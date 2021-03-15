package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;

@UtilityClass
public class BursarFeesFinesUtils {

  public static final String CHARGE_FEESFINES_EXPORT_STEP = "CHARGE_FEESFINES_EXPORT_STEP";
  public static final String REFUND_FEESFINES_EXPORT_STEP = "REFUND_FEESFINES_EXPORT_STEP";

  private static final String CHARGE_FILE_NAME_PATTERN = "lib_%sa.dat";
  private static final String REFUND_FILE_NAME_PATTERN = "lib_%sb.dat";
  private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
  private final SimpleDateFormat TRANSACTION_DATE_TIME_FORMAT = new SimpleDateFormat("MMddyy");

  public static String getFilename(String stepName) {
    String fileNamePattern;
    if (CHARGE_FEESFINES_EXPORT_STEP.equals(stepName)) {
      fileNamePattern = CHARGE_FILE_NAME_PATTERN;
    } else if (REFUND_FEESFINES_EXPORT_STEP.equals(stepName)) {
      fileNamePattern = REFUND_FILE_NAME_PATTERN;
    } else {
      throw new IllegalArgumentException(String.format("Can't create file name for step %s", stepName));
    }

    return String.format(fileNamePattern, LocalDateTime.now().format(FILENAME_DATE_TIME_FORMATTER));
  }

  public static String getItemType() {
    return StringUtils.rightPad(" ", 12);
  }

  public static String getItemTypeDescription(String feeFineType) {
    return StringUtils.rightPad(feeFineType, 30);
  }

  public static String normalizeAmount(BigDecimal amount) {
    return StringUtils.leftPad(amount.setScale(2).toString(), 9, "0");
  }

  public static String getTransactionDate(Date createdDate) {
    return TRANSACTION_DATE_TIME_FORMAT.format(createdDate);
  }

  public static String getEmployeeId(String userId, Map<String, String> userIdMap) {
    String externalId = userIdMap.get(userId);
    externalId = externalId == null ? "" : externalId.substring(externalId.length() - 7);
    return StringUtils.rightPad(externalId, 11);
  }

}
