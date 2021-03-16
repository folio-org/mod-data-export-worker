package org.folio.dew.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.JobParameterNames;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class CsvPartStepExecutionListener implements StepExecutionListener {

  private final MinIOObjectStorageRepository repository;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    // Nothing to do
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    String filename = stepExecution.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);

    try {
      repository.uploadObject(FilenameUtils.getName(filename), filename, null, "text/csv");
    } catch (Exception e) {
      log.error(e.toString(), e);
      stepExecution.getJobExecution().addFailureException(e);
      return ExitStatus.FAILED;
    }

    return stepExecution.getExitStatus();
  }

}
