package org.folio.dew.batch.bursarfeesfines;

import java.io.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarFeesFinesUtils;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class BursarExportStepListener implements StepExecutionListener {

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

    String downloadFilename = BursarFeesFinesUtils.getFilename(stepExecution.getStepName());
    var jobExecution = stepExecution.getJobExecution();
    String filename = jobExecution.getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) + '_' + downloadFilename;
    if (!new File(filename).exists()) {
      log.error("Can't find {}.", filename);
      return ExitStatus.FAILED;
    }

    String url;
    try {
      url = repository.objectWriteResponseToPresignedObjectUrl(
          repository.uploadObject(FilenameUtils.getName(filename), filename, downloadFilename, MediaType.TEXT_MARKDOWN_VALUE));
    } catch (Exception e) {
      log.error(e.toString(), e);
      jobExecution.addFailureException(e);
      return ExitStatus.FAILED;
    }

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, url, ";");

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.JOB_DESCRIPTION,
        String.format(BursarFeesFinesUtils.getJobDescriptionPart(stepExecution.getStepName()), stepExecution.getWriteCount()),
        "\n");

    return exitStatus;
  }

}
