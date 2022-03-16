package org.folio.dew.utils;

import lombok.experimental.UtilityClass;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;

@UtilityClass
public class BulkEditProcessorHelper {
  private static DateFormat dateFormat;
  private static Map<String, String> identifiersMap = new HashMap<>();

  static {
    dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    identifiersMap.put("ID", "id");
    identifiersMap.put("BARCODE", "barcode");
    identifiersMap.put("HRID", "hrid");
    identifiersMap.put("FORMER_IDS", "formerIds");
    identifiersMap.put("ACCESSION_NUMBER", "accessionNumber");
    identifiersMap.put("HOLDINGS_RECORD_ID", "holdingsRecordId");
  }

  public static String dateToString(Date date) {
    return nonNull(date) ? dateFormat.format(date) : EMPTY;
  }

  public static String resolveIdentifier(String identifier) {
    return identifiersMap.get(identifier);
  }
}
