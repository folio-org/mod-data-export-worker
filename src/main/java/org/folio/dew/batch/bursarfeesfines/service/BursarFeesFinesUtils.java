package org.folio.dew.batch.bursarfeesfines.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.BursarFeeFines;
import org.folio.dew.domain.dto.BursarFeeFinesTypeMapping;
import org.folio.dew.domain.dto.Account;

@UtilityClass
public class BursarFeesFinesUtils {

  public static final String CHARGE_FEESFINES_EXPORT_STEP = "CHARGE_FEESFINES_EXPORT_STEP";
  public static final String REFUND_FEESFINES_EXPORT_STEP = "REFUND_FEESFINES_EXPORT_STEP";

  private static final Map<String, String> FILE_PATTERNS = new HashMap<>();

  static {
    FILE_PATTERNS.put(CHARGE_FEESFINES_EXPORT_STEP, "lib_%sa.dat");
    FILE_PATTERNS.put(REFUND_FEESFINES_EXPORT_STEP, "lib_%sb.dat");
  }

  private static final Map<String, String> JOB_DESCRIPTION_PATTERNS = new HashMap<>();

  static {
    JOB_DESCRIPTION_PATTERNS.put(CHARGE_FEESFINES_EXPORT_STEP, "# of charges: %d");
    JOB_DESCRIPTION_PATTERNS.put(REFUND_FEESFINES_EXPORT_STEP, "# of refunds: %d");
  }

  private static final DateTimeFormatter FILENAME_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
  private static final ThreadLocal<SimpleDateFormat> TRANSACTION_DATE_TIME_FORMAT = ThreadLocal
    .withInitial(() -> new SimpleDateFormat("MMddyy"));

  public static String getFilename(String stepName) {
    return String.format(FILE_PATTERNS.get(stepName), LocalDateTime.now().format(FILENAME_DATE_TIME_FORMATTER));
  }

  public static String getJobDescriptionPart(String stepName) {
    return JOB_DESCRIPTION_PATTERNS.get(stepName);
  }

  public static String formatItemType(String type) {
    return StringUtils.rightPad(type, 12);
  }

  public static String formatItemTypeDescription(String description) {
    return StringUtils.rightPad(description, 30);
  }

  public static String normalizeAmount(BigDecimal amount) {
    return StringUtils.leftPad(amount.setScale(2, RoundingMode.UNNECESSARY).toString(), 9, "0");
  }

  public static String getTransactionDate(Date createdDate) {
    return TRANSACTION_DATE_TIME_FORMAT.get().format(createdDate);
  }

  /**
   * Clean up the ThreadLocal.
   */
  public static void cleanUp() {
    TRANSACTION_DATE_TIME_FORMAT.remove();
  }

  public static String getEmployeeId(String userId, Map<String, String> userIdMap) {
    String externalId = userIdMap.get(userId);
    if (StringUtils.isEmpty(externalId)) {
      externalId = "";
    } else if (externalId.length() > 7) {
      externalId = externalId.substring(externalId.length() - 7);
    }
    return StringUtils.rightPad(externalId, 11);
  }

  public static BursarFeeFinesTypeMapping getMapping(BursarFeeFines bursarFeeFines, Account account) {
    if (bursarFeeFines.getTypeMappings() == null) {
      return null;
    }

    final List<BursarFeeFinesTypeMapping> feeFinesTypeMappingList = bursarFeeFines
      .getTypeMappings()
      .get(account.getOwnerId());

    if (feeFinesTypeMappingList == null) {
      return null;
    }

    return feeFinesTypeMappingList
        .stream()
        .filter(m -> m.getFeefineTypeId().toString().equals(account.getFeeFineId()))
        .findFirst()
        .orElse(null);
  }
}
