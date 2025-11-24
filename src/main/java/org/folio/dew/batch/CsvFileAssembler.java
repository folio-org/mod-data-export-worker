package org.folio.dew.batch;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class CsvFileAssembler implements StepExecutionAggregator {
  private static final String TEXT_CSV = "text/csv";

  private final RemoteFilesStorage remoteFilesStorage;

  @Override
  public void aggregate(StepExecution stepExecution, Collection<StepExecution> finishedStepExecutions) {
    var csvFilePartObjectNames = finishedStepExecutions.stream()
        .map(e -> e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH))
        .toList();
    var destCsvObject = FilenameUtils.getName(
        stepExecution.getJobExecution().getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) + ".csv");
    try {
      if ("CIRCULATION_LOG".equals(stepExecution.getJobExecution().getJobInstance().getJobName())) {
        var csvUrl = remoteFilesStorage.compose(destCsvObject, csvFilePartObjectNames, null, TEXT_CSV);
        ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.CIRCULATION_LOG_FILE_NAME, destCsvObject, ";");
        ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, csvUrl, ";");
      } else {
        var prefix = stepExecution.getJobExecution().getJobParameters().getString(JobParameterNames.JOB_ID) + "/";

        destCsvObject = prefix + destCsvObject;
        var csvUrl = remoteFilesStorage.objectToPresignedObjectUrl(
          remoteFilesStorage.compose(destCsvObject, csvFilePartObjectNames, null, TEXT_CSV));

        var jsonFilePartObjectNames = finishedStepExecutions.stream()
          .map(e -> e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) + ".json")
          .toList();
        var destJsonObject = prefix + FilenameUtils.getName(
          stepExecution.getJobExecution().getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) + ".json");
        var jsonUrl = remoteFilesStorage.objectToPresignedObjectUrl(
          remoteFilesStorage.compose(destJsonObject, jsonFilePartObjectNames, null, TEXT_CSV));

        ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, csvUrl + ";;" + jsonUrl, ";");
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}
