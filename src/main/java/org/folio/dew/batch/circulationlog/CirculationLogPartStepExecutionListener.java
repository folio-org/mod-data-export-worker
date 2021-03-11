package org.folio.dew.batch.circulationlog;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.ExecutionContextUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@RequiredArgsConstructor
public class CirculationLogPartStepExecutionListener implements StepExecutionListener {

  private final MinIOObjectStorageRepository objectStorageRepository;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    // Nothing to do
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    ExecutionContext executionContext = stepExecution.getExecutionContext();
    String outputFilePath = executionContext.getString(JobParameterNames.OUTPUT_FILE_PATH);
    String objectName = ExecutionContextUtils.getObjectNameByOutputFilePath(executionContext);

    try {
      objectStorageRepository.uploadObject(objectName, outputFilePath, "csv");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return stepExecution.getExitStatus();
  }

}
