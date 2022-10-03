package org.folio.dew.domain.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JobParameterNames {

  public static final String JOB_ID = "jobId";
  public static final String JOB_NAME = "jobName";
  public static final String JOB_DESCRIPTION = "jobDescription";
  public static final String TEMP_OUTPUT_FILE_PATH = "tempOutputFilePath";
  public static final String OUTPUT_FILES_IN_STORAGE = "outputFilesInStorage";
  public static final String TOTAL_RECORDS = "totalRecords";
  public static final String UPDATED_FILE_NAME = "updatedFileName";
  public static final String PREVIEW_FILE_NAME = "previewFileName";
  public static final String UPLOADED_FILE_PATH = "uploadedFilePath";
  public static final String EDIFACT_FILE_NAME = "edifactFileName";
  public static final String QUERY = "query";
  public static final String EDIFACT_ORDER_EXPORT = "edifactOrdersExport";

}
