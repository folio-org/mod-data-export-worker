package org.folio.dew.model.circulationlog;

import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.entity.constants.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.ExecutionContextUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class CirculationLogPartStepExecutionListener implements StepExecutionListener {

  private final MinIOObjectStorageRepository objectStorageRepository;
  private final String workspaceBucketName;

  @Autowired
  public CirculationLogPartStepExecutionListener(MinIOObjectStorageRepository objectStorageRepository,
      @Value("${minio.workspaceBucketName}") String workspaceBucketName) {
    this.objectStorageRepository = objectStorageRepository;
    this.workspaceBucketName = workspaceBucketName;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {

    final String csvFilePartContentType = "csv";

    ExecutionContext executionContext = stepExecution.getExecutionContext();
    String outputFilePath = executionContext.getString(JobParameterNames.OUTPUT_FILE_PATH);
    String objectName = ExecutionContextUtils.getObjectNameByOutputFilePath(executionContext);

    try {
      objectStorageRepository.uploadObject(this.workspaceBucketName, objectName, outputFilePath, csvFilePartContentType);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }

    return stepExecution.getExitStatus();
  }

}
