package org.folio.dew.batch.bursarfeesfines;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.BursarFilenameUtil;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@StepScope
@RequiredArgsConstructor
public class BursarExportStepListener implements StepExecutionListener {

  private final MinIOObjectStorageRepository objectStorageRepository;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    // Nothing to do
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    JobExecution execution = stepExecution.getJobExecution();
    String objectName = BursarFilenameUtil.getFilename(stepExecution.getStepName());
    String outputFilePath = execution.getJobParameters().getString(JobParameterNames.OUTPUT_FILE_PATH) + objectName;

    try {
      objectStorageRepository.uploadObject(objectName, outputFilePath, "dat");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return stepExecution.getExitStatus();
  }

}
