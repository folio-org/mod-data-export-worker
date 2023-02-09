package org.folio.dew.batch.authoritycontrol;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.BaseStepListener;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.error.NotFoundException;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.folio.dew.batch.ExecutionContextUtils.addToJobExecutionContext;
import static org.folio.dew.utils.Constants.AUTHORITY_CONTROL_EXPORT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;

@Log4j2
@Component
public class AuthorityControlStepListener extends BaseStepListener {
  private static final String DELIMITER = ";";

  private final FolioExecutionContext folioExecutionContext;

  @Value("${spring.application.name}")
  protected String springApplicationName;

  public AuthorityControlStepListener(RemoteFilesStorage remoteFilesStorage, LocalFilesStorage localFilesStorage,
                                      FolioExecutionContext folioExecutionContext) {
    super(remoteFilesStorage, localFilesStorage);
    this.folioExecutionContext = folioExecutionContext;
  }

  @Override
  public ExitStatus afterStepExecution(StepExecution stepExecution) {
    try {
      var tempFilePath = getTempFile(stepExecution);
      var fileName = FilenameUtils.getName(tempFilePath);
      var fullFilePath = buildFullFilePath(fileName);

      var uploadedFilePath = super.getRemoteFilesStorage()
        .uploadObject(fullFilePath, tempFilePath, null, "text/csv", true);

      addToJobExecutionContext(stepExecution, JobParameterNames.AUTHORITY_CONTROL_FILE_NAME, fileName, DELIMITER);
      addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, uploadedFilePath, DELIMITER);

      return stepExecution.getExitStatus();
    } catch (Exception e) {
      log.error(e.toString(), e);
      stepExecution.getJobExecution().addFailureException(e);
      return ExitStatus.FAILED;
    }
  }

  private String getTempFile(StepExecution stepExecution) throws NotFoundException {
    var tempFilePath = stepExecution.getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    if (super.getLocalFilesStorage().notExists(tempFilePath)) {
      throw new NotFoundException("Can't find " + tempFilePath);
    }
    return tempFilePath;
  }

  private String buildFullFilePath(String fileName) {
    var workDir = getWorkingDirectory(springApplicationName, AUTHORITY_CONTROL_EXPORT_DIR_NAME);
    var tenantName = folioExecutionContext.getTenantId();

    return String.format("%s%s/%s", workDir, tenantName, fileName);
  }
}
