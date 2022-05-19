package org.folio.dew.utils;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.IdentifierType.ACCESSION_NUMBER;
import static org.folio.dew.domain.dto.IdentifierType.BARCODE;
import static org.folio.dew.domain.dto.IdentifierType.EXTERNAL_SYSTEM_ID;
import static org.folio.dew.domain.dto.IdentifierType.FORMER_IDS;
import static org.folio.dew.domain.dto.IdentifierType.HOLDINGS_RECORD_ID;
import static org.folio.dew.domain.dto.IdentifierType.HRID;
import static org.folio.dew.domain.dto.IdentifierType.ID;
import static org.folio.dew.domain.dto.IdentifierType.USER_NAME;
import static org.folio.dew.utils.Constants.DATE_TIME_PATTERN;

import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.IdentifierType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.TimeZone;

@UtilityClass
public class BulkEditProcessorHelper {
  private static final DateFormat dateFormat;
  private static final EnumMap<IdentifierType, String> identifiersMap = new EnumMap<>(IdentifierType.class);

  static {
    dateFormat = new SimpleDateFormat(DATE_TIME_PATTERN);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    identifiersMap.put(ID, "id");
    identifiersMap.put(BARCODE, "barcode");
    identifiersMap.put(HRID, "hrid");
    identifiersMap.put(FORMER_IDS, "formerIds");
    identifiersMap.put(ACCESSION_NUMBER, "accessionNumber");
    identifiersMap.put(HOLDINGS_RECORD_ID, "holdingsRecordId");
    identifiersMap.put(USER_NAME, "username");
    identifiersMap.put(EXTERNAL_SYSTEM_ID, "externalSystemId");
  }

  public static String dateToString(Date date) {
    return nonNull(date) ? dateFormat.format(date) : EMPTY;
  }

  public static String resolveIdentifier(String identifier) {
    return identifiersMap.get(IdentifierType.fromValue(identifier));
  }
}
