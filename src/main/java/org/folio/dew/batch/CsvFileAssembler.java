package org.folio.dew.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.des.domain.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.ExecutionContextUtils;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Log4j2
@RequiredArgsConstructor
public class CsvFileAssembler implements StepExecutionAggregator {

  private final MinIOObjectStorageRepository repository;

  @Override
  public void aggregate(StepExecution stepExecution, Collection<StepExecution> finishedStepExecutions) {
    List<String> csvFilePartObjectNames = finishedStepExecutions.stream()
        .map(e -> FilenameUtils.getName(e.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH)))
        .collect(Collectors.toList());
    String destObject = FilenameUtils.getName(
        stepExecution.getJobExecution().getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) + ".csv");

    String url;
    try {
      url = repository.objectWriteResponseToPresignedObjectUrl(
          repository.composeObject(destObject, csvFilePartObjectNames, null, "text/csv"));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, url);
  }

}
