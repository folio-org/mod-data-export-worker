package org.folio.dew.model;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.entity.constants.JobParameterNames;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.ExecutionContextUtils;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@Log4j2
public class CsvFileAssembler implements StepExecutionAggregator {

  private final MinIOObjectStorageRepository minIOObjectStorageRepository;
  private final String workspaceBucketName;

  public CsvFileAssembler(MinIOObjectStorageRepository minIOObjectStorageRepository,
      @Value("${minio.workspaceBucketName}") String workspaceBucketName) {
    this.minIOObjectStorageRepository = minIOObjectStorageRepository;
    this.workspaceBucketName = workspaceBucketName;
  }

  @Override
  public void aggregate(StepExecution stepExecution, Collection<StepExecution> finishedStepExecutions) {

    List<String> csvFilePartObjectNames = new ArrayList<>();
    for (StepExecution currentFinishedStepExecution : finishedStepExecutions) {
      ExecutionContext executionContext = currentFinishedStepExecution.getExecutionContext();
      String objectName = ExecutionContextUtils.getObjectNameByOutputFilePath(executionContext);
      csvFilePartObjectNames.add(objectName);
    }

    JobParameters jobParameters = stepExecution.getJobExecution().getJobParameters();
    String outputFilePath = jobParameters.getString(JobParameterNames.OUTPUT_FILE_PATH);
    String csvObjectName = FilenameUtils.getName(outputFilePath);

    try {
      minIOObjectStorageRepository.composeObject(workspaceBucketName, csvObjectName, csvFilePartObjectNames);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }

}
