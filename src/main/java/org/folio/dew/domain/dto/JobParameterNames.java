package org.folio.dew.domain.dto;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JobParameterNames {

  public static final String JOB_ID = "jobId";
  public static final String JOB_NAME = "jobName";
  public static final String JOB_DESCRIPTION = "jobDescription";
  public static final String TEMP_OUTPUT_FILE_PATH = "tempOutputFilePath";
  public static final String TEMP_LOCAL_FILE_PATH = "temporaryLocalFile";
  public static final String OUTPUT_FILES_IN_STORAGE = "outputFilesInStorage";
  public static final String TOTAL_RECORDS = "totalRecords";
  public static final String UPDATED_FILE_NAME = "updatedFileName";
  public static final String PREVIEW_FILE_NAME = "previewFileName";
  public static final String UPLOADED_FILE_PATH = "uploadedFilePath";
  public static final String ACQ_EXPORT_FILE = "acqExportFile";
  public static final String ACQ_EXPORT_FILE_NAME = "acqExportFileName";
  public static final String E_HOLDINGS_FILE_NAME = "eHoldingsFileName";
  public static final String CIRCULATION_LOG_FILE_NAME = "circulationLogFileName";
  public static final String AUTHORITY_CONTROL_FILE_NAME = "authorityControlFileName";
  public static final String BURSAR_FEES_FINES_FILE_NAME = "bursarFeesFinesFileName";
  public static final String QUERY = "query";
  public static final String EDIFACT_ORDERS_EXPORT = "edifactOrdersExport";
  public static final String TEMP_OUTPUT_MARC_PATH = "tempOutputMarcPath";
  public static final String TEMP_OUTPUT_CSV_PATH = "tempOutputCsvPath";
  public static final String TEMP_OUTPUT_JSON_PATH = "tempOutputJsonPath";
  public static final String TEMP_LOCAL_MARC_PATH = "tempLocalMarcPath";
  public static final String AT_LEAST_ONE_MARC_EXISTS = "atLeastOneMarcExists";
}
