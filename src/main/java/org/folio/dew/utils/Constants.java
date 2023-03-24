package org.folio.dew.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public static final int CHUNKS = 100;
  public static final String ROLLBACK_FILE = "rollBackFile";
  public static final String TMP_DIR_PROPERTY = "java.io.tmpdir";
  public static final String PATH_SEPARATOR = "/";
  public static final String JOB_ID_SEPARATOR = "_";
  public static final String JOB_NAME_POSTFIX_SEPARATOR = "-";
  public static final String MATCHED_RECORDS = "-Matched-Records-";
  public static final String CHANGED_RECORDS = "-Changed-Records-";
  public static final String UPDATED_PREFIX = "UPDATED-";
  public static final String PREVIEW_PREFIX = "PREVIEW-";
  public static final String INITIAL_PREFIX = "INITIAL-";
  public static final String CSV_EXTENSION = ".csv";

  public static final String ARRAY_DELIMITER = ";";
  public static final String ELECTRONIC_RELATIONSHIP_NAME_ID_DELIMITER = ARRAY_DELIMITER;
  public static final String ITEM_DELIMITER = "|";
  public static final String ITEM_DELIMITER_PATTERN = "\\|";
  public static final String KEY_VALUE_DELIMITER = ":";

  public static final String FILE_NAME = "fileName";
  public static final String TEMP_IDENTIFIERS_FILE_NAME = "tempIdentifiersFileName";
  public static final String TOTAL_CSV_LINES = "totalCsvLines";
  public static final String NUMBER_OF_WRITTEN_RECORDS = "numberOfWrittenRecords";
  public static final String JOB_ID = "jobId";
  public static final String EXPORT_TYPE = "exportType";
  public static final String ENTITY_TYPE = "entityType";
  public static final String IDENTIFIER_TYPE = "identifierType";
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");
  public static final String LINE_SEPARATOR_REPLACEMENT = "\\n";


  public static final String LINE_BREAK = "\n";
  public static final String LINE_BREAK_REPLACEMENT = "\\n";
  public static final String COMMA = ",";
  public static final String QUOTE = "\"";
  public static final String QUOTE_REPLACEMENT = "\"\"";

  public static final String NO_MATCH_FOUND_MESSAGE = "No match found";
  public static final String NO_CHANGE_MESSAGE = "No change in value needed";
  public static final String STATUS_FIELD_CAN_NOT_CLEARED = "Status field can not be cleared";
  public static final String STATUS_VALUE_NOT_ALLOWED = "New status value \"%s\" is not allowed";

  public static final String MODULE_NAME = "BULKEDIT";
  public static final String BULKEDIT_DIR_NAME = "bulk_edit";
  public static final String EDIFACT_EXPORT_DIR_NAME = "edifact_export";
  public static final String E_HOLDINGS_EXPORT_DIR_NAME = "e_holdings_export";
  public static final String AUTHORITY_CONTROL_EXPORT_DIR_NAME = "authority_control_export";
  public static final String STATUSES_CONFIG_NAME = "statuses";
  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
  public static final String FILE_UPLOAD_ERROR = "Cannot upload a file. Reason: %s.";

  public static String getWorkingDirectory(String springApplicationName, String dirName) {
    return springApplicationName + PATH_SEPARATOR + dirName + PATH_SEPARATOR;
  }
}
