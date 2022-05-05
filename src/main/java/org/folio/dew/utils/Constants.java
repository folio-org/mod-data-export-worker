package org.folio.dew.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String ROLLBACK_FILE = "rollBackFile";
  public static final String TMP_DIR_PROPERTY = "java.io.tmpdir";
  public static final String PATH_SEPARATOR = "/";
  public static final String JOB_ID_SEPARATOR = "_";
  public static final String JOB_NAME_POSTFIX_SEPARATOR = "-";
  public static final String MATCHED_RECORDS = "-Matched-Records-";
  public static final String CHANGED_RECORDS = "-Changed-Records-";
  public static final String UPDATED_PREFIX = "UPDATED-";
  public static final String CSV_EXTENSION = ".csv";

  public static final String ARRAY_DELIMITER = ";";
  public static final String ITEM_DELIMITER = "|";
  public static final String ITEM_DELIMITER_PATTERN = "\\|";
  public static final String KEY_VALUE_DELIMITER = ":";

  public static final String FILE_NAME = "fileName";
  public static final String JOB_ID = "jobId";
  public static final String EXPORT_TYPE = "exportType";
  public static final String ENTITY_TYPE = "entityType";
  public static final String IDENTIFIER_TYPE = "identifierType";
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSX";
  public static final String PREVIEW_USERS_QUERY = "previewUsersQuery";

  public static final String LINE_BREAK = "\n";
  public static final String LINE_BREAK_REPLACEMENT = "\\n";

  public static final String NO_MATCH_FOUND_MESSAGE = "No match found";

  public static final String MODULE_NAME = "BULKEDIT";
  public static final String STATUSES_CONFIG_NAME = "statuses";
  public static final String BULK_EDIT_CONFIGURATIONS_QUERY_TEMPLATE = "module==%s and configName==%s";
}
