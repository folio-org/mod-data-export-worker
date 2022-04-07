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

}
