package org.folio.dew.batch.bursarfeesfines;

import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.entity.constant.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.BursarFilenameUtil;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@StepScope
public class BursarExportStepListener implements StepExecutionListener {

  private final MinIOObjectStorageRepository objectStorageRepository;
  private final String workspaceBucketName;

  @Autowired
  public BursarExportStepListener(
      MinIOObjectStorageRepository objectStorageRepository,
      @Value("${minio.workspaceBucketName}") String workspaceBucketName) {
    this.objectStorageRepository = objectStorageRepository;
    this.workspaceBucketName = workspaceBucketName;
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {}

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {

    final String fileContentType = "dat";
    JobExecution execution = stepExecution.getJobExecution();
    String objectName = BursarFilenameUtil.getFilename(stepExecution.getStepName());
    String outputFilePath =
        execution.getJobParameters().getString(JobParameterNames.OUTPUT_FILE_PATH) + objectName;

    try {
      objectStorageRepository.uploadObject(
          workspaceBucketName, objectName, outputFilePath, fileContentType);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }

    return stepExecution.getExitStatus();
  }
}
