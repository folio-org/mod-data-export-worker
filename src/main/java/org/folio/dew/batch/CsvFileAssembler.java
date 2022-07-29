package org.folio.dew.batch;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class CsvFileAssembler implements StepExecutionAggregator {

  private final RemoteFilesStorage remoteFilesStorage;

  @Override
  public void aggregate(StepExecution stepExecution, Collection<StepExecution> finishedStepExecutions) {
    List<String> csvFilePartObjectNames = finishedStepExecutions.stream()
        .map(e -> e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH))
        .collect(Collectors.toList());
    String destObject = FilenameUtils.getName(
        stepExecution.getJobExecution().getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) + ".csv");

    String url;
    try {
      url = remoteFilesStorage.objectWriteResponseToPresignedObjectUrl(
          remoteFilesStorage.composeObject(destObject, csvFilePartObjectNames, null, "text/csv"));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, url, ";");
  }

}
