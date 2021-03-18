package org.folio.dew.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.des.domain.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.io.File;

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
    ExitStatus exitStatus = stepExecution.getExitStatus();
    if (ExitStatus.FAILED.getExitCode().equals(exitStatus.getExitCode())) {
      return exitStatus;
    }

    String filename = stepExecution.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    if (!new File(filename).exists()) {
      log.error("Can't find {}.", filename);
      return ExitStatus.FAILED;
    }

    try {
      repository.uploadObject(FilenameUtils.getName(filename), filename, null, "text/csv");
    } catch (Exception e) {
      log.error(e.toString(), e);
      stepExecution.getJobExecution().addFailureException(e);
      return ExitStatus.FAILED;
    }

    return exitStatus;
  }

}
