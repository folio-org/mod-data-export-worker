package org.folio.dew.utils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class FormatterUtils {

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
    final SimpleDateFormat dateFormat = new SimpleDateFormat("MMddyy");
    return dateFormat.format(createdDate);
  }

  public static String getEmployeeId(String userId, Map<String, String> userIdMap) {
    String externalId = userIdMap.get(userId);
    externalId = externalId == null ? "" : externalId.substring(externalId.length() - 7);
    return StringUtils.rightPad(externalId, 11);
  }
}
