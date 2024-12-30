package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.folio.dew.utils.Constants.EDIFACT_EXPORT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;

import java.nio.charset.StandardCharsets;

import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.exceptions.EdifactException;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@SuperBuilder
@Component
@StepScope
@Log4j2
public class SaveToMinioTasklet extends FilterableTasklet {

  private static final String REMOTE_STORAGE_ERROR_MESSAGE = "Failed to save edifact file to remote storage";
  private static final String UPLOADED_PATH_TEMPLATE = "%s%s/%s";

  private final RemoteFilesStorage remoteFilesStorage;
  private final FolioExecutionContext folioExecutionContext;

  @Value("${spring.application.name}")
  protected String springApplicationName;

  @Override
  @SneakyThrows
  public RepeatStatus execute(VendorEdiOrdersExportConfig exportConfig, StepContribution contribution, ChunkContext chunkContext) {
    // retrieve parameters from job context
    var stepExecution = chunkContext.getStepContext().getStepExecution();
    var edifactOrderAsString = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE);
    var fullFilePath = buildFullFilePath(stepExecution);
    String uploadedFilePath;
    try {
      uploadedFilePath = remoteFilesStorage.write(fullFilePath, edifactOrderAsString.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      log.error(REMOTE_STORAGE_ERROR_MESSAGE, e);
      throw new EdifactException(REMOTE_STORAGE_ERROR_MESSAGE);
    }
    ExecutionContextUtils.addToJobExecutionContext(contribution.getStepExecution(), OUTPUT_FILES_IN_STORAGE, uploadedFilePath, ";");

    return RepeatStatus.FINISHED;
  }

  private String buildFullFilePath(StepExecution stepExecution) {
    var workDir = getWorkingDirectory(springApplicationName, EDIFACT_EXPORT_DIR_NAME);
    var tenantName = folioExecutionContext.getTenantId();
    var fileName = (String) ExecutionContextUtils.getExecutionVariable(stepExecution, ACQ_EXPORT_FILE_NAME);
    return UPLOADED_PATH_TEMPLATE.formatted(workDir, tenantName, fileName);
  }

}
