package org.folio.dew.batch.eholdings;

import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class EHoldingsStepListener implements StepExecutionListener {

  private final MinIOObjectStorageRepository repository;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    // Nothing to do
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    var exitStatus = stepExecution.getExitStatus();
    if (ExitStatus.FAILED.getExitCode().equals(exitStatus.getExitCode())) {
      return exitStatus;
    }

    var filename = stepExecution.getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    if (!new File(filename).exists()) {
      log.error("Can't find {}.", filename);
      return ExitStatus.FAILED;
    }

    String url;
    try {
      url = repository.objectWriteResponseToPresignedObjectUrl(
        repository.uploadObject(FilenameUtils.getName(filename), filename, null, "text/csv", true));
    } catch (Exception e) {
      log.error(e.toString(), e);
      stepExecution.getJobExecution().addFailureException(e);
      return ExitStatus.FAILED;
    }

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, url, ";");

    return exitStatus;
  }
}
