package org.folio.dew.utils;

import lombok.experimental.UtilityClass;
import org.folio.dew.domain.dto.EntityType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.folio.dew.domain.dto.EntityType.HOLDINGS_RECORD;
import static org.folio.dew.domain.dto.EntityType.INSTANCE;
import static org.folio.dew.domain.dto.EntityType.ITEM;

@UtilityClass
public class Constants {
  public static final String LINKED_DATA_SOURCE = "LINKED_DATA";
  public static final String MARC_SOURCE = "MARC";

  public static final String ROLLBACK_FILE = "rollBackFile";
  public static final String PATH_SEPARATOR = "/";
  public static final String JOB_NAME_POSTFIX_SEPARATOR = "-";
  public static final String STATISTICAL_CODE_NAME_SEPARATOR = " - ";
  public static final String MATCHED_RECORDS = "-Matched-Records-";
  public static final String MARC_RECORDS = "-Marc-Records-";
  public static final String CHANGED_RECORDS = "-Changed-Records-";
  public static final String UPDATED_PREFIX = "UPDATED-";
  public static final String INITIAL_PREFIX = "INITIAL-";
  public static final String CSV_EXTENSION = ".csv";

  public static final String ARRAY_DELIMITER = ";";
  public static final String ARRAY_DELIMITER_SPACED = "; ";
  public static final String ARRAY_DELIMITER_WITH_HIDDEN_SYMBOL = "\u001f;";
  public static final String ITEM_DELIMITER = "|";
  public static final String ITEM_DELIMITER_SPACED = " | ";
  public static final String KEY_VALUE_DELIMITER = ":";
  public static final String VERTICAL_BAR_WITH_HIDDEN_SYMBOL = "\u001f|";
  public static final String EMPTY_ELEMENT = "-";
  public static final Map<EntityType, String> ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DELIMITER = Map.of(HOLDINGS_RECORD,
      VERTICAL_BAR_WITH_HIDDEN_SYMBOL, ITEM, ITEM_DELIMITER, INSTANCE,
      VERTICAL_BAR_WITH_HIDDEN_SYMBOL);
  public static final Map<EntityType, String> ENTITY_TYPE_TO_ELECTRONIC_ACCESS_DATA_DELIMITER = Map.of(HOLDINGS_RECORD,
      ARRAY_DELIMITER_WITH_HIDDEN_SYMBOL, ITEM, ARRAY_DELIMITER, INSTANCE,
      ARRAY_DELIMITER_WITH_HIDDEN_SYMBOL);

  public static final String FILE_NAME = "fileName";
  public static final String TEMP_IDENTIFIERS_FILE_NAME = "tempIdentifiersFileName";
  public static final String TOTAL_CSV_LINES = "totalCsvLines";
  public static final String NUMBER_OF_PROCESSED_IDENTIFIERS = "numberOfProcessedIdentifiers";
  public static final String JOB_ID = "jobId";
  public static final String EXPORT_TYPE = "exportType";
  public static final String ENTITY_TYPE = "entityType";
  public static final String IDENTIFIER_TYPE = "identifierType";
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";
  public static final String DATE_WITHOUT_TIME_PATTERN = "yyyy-MM-dd";

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  public static final String LINE_SEPARATOR_REPLACEMENT = "\\n";


  public static final String LINE_BREAK = "\n";
  public static final String LINE_BREAK_REPLACEMENT = "\\n";
  public static final String COMMA = ",";
  public static final String QUOTE = "\"";
  public static final String QUOTE_REPLACEMENT = "\"\"";
  public static final char NEW_LINE = '\n';

  public static final String NO_MATCH_FOUND_MESSAGE = "No match found";
  public static final String MULTIPLE_MATCHES_MESSAGE = "Multiple matches for the same identifier.";
  public static final String NO_MARC_CONTENT = "Cannot get marc content for record with id = %s, reason: %s";
  public static final String NO_ITEM_AFFILIATION = "User %s does not have required affiliation to view the item record - %s=%s on the tenant %s";
  public static final String NO_ITEM_VIEW_PERMISSIONS = "User %s does not have required permission to view the item record - %s=%s on the tenant %s";
  public static final String NO_HOLDING_AFFILIATION = "User %s does not have required affiliation to view the holdings record - %s=%s on the tenant %s";
  public static final String NO_HOLDING_VIEW_PERMISSIONS = "User %s does not have required permission to view the holdings record - %s=%s on the tenant %s";
  public static final String NO_INSTANCE_VIEW_PERMISSIONS = "User %s does not have required permission to view the instance record - %s=%s on the tenant %s";
  public static final String NO_USER_VIEW_PERMISSIONS = "User %s does not have required permission to view the user record - %s=%s on the tenant %s";
  public static final String DUPLICATES_ACROSS_TENANTS = "Duplicates across tenants";
  public static final String DUPLICATE_ENTRY = "Duplicate entry";
  public static final String CANNOT_GET_RECORD = "Cannot get data from %s due to %s";
  public static final String LINKED_DATA_SOURCE_IS_NOT_SUPPORTED = "Bulk edit of instances with source set to LINKED_DATA is not supported.";
  public static final String MULTIPLE_SRS = "Multiple SRS records are associated with the instance. The following SRS have been identified: %s.";
  public static final String SRS_MISSING = "SRS record associated with the instance is missing.";

  public static final String MODULE_NAME = "BULKEDIT";
  public static final String BULKEDIT_DIR_NAME = "bulk_edit";
  public static final String EDIFACT_EXPORT_DIR_NAME = "edifact_export";
  public static final String E_HOLDINGS_EXPORT_DIR_NAME = "e_holdings_export";
  public static final String AUTHORITY_CONTROL_EXPORT_DIR_NAME = "authority_control_export";
  public static final String STATUSES_CONFIG_NAME = "statuses";
  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
  public static final String FILE_UPLOAD_ERROR = "Cannot upload a file. Reason: %s.";
  public static final String STAFF_ONLY = "(staff only)";
  public static final String HOLDINGS_LOCATION_CALL_NUMBER_DELIMITER = " > ";

  public static final String PERMANENT_LOCATION_ID = "permanentLocationId";
  public static final String CALL_NUMBER_PREFIX = "callNumberPrefix";
  public static final String CALL_NUMBER = "callNumber";
  public static final String CALL_NUMBER_SUFFIX = "callNumberSuffix";
  public static final String PATH_TO_ERRORS = "PATH_TO_ERRORS";
  public static final String PATH_TO_MATCHED_RECORDS = "PATH_TO_MATCHED_RECORDS";
  public static final String CHARACTERS_SHOULD_BE_REPLACED_IN_PATH = "[+]";

  public static final String EUREKA_PLATFORM = "eureka";
  public static final String OKAPI_PLATFORM = "okapi";

  public static final String QUERY_PATTERN_REF_ID = "refId==%s";

  public static final String UTF8_BOM = new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8);

  public static final int MIN_YEAR_FOR_BIRTH_DATE = 1900;

  public static String getWorkingDirectory(String springApplicationName, String dirName) {
    return springApplicationName + PATH_SEPARATOR + dirName + PATH_SEPARATOR;
  }
}
