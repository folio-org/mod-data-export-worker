package org.folio.dew.batch.eholdings;

import static org.folio.dew.utils.Constants.E_HOLDINGS_EXPORT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.BaseStepListener;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Log4j2
@Component
public class EHoldingsStepListener extends BaseStepListener {

  private final FolioExecutionContext folioExecutionContext;

  @Value("${spring.application.name}")
  protected String springApplicationName;

  public EHoldingsStepListener(RemoteFilesStorage remoteFilesStorage, LocalFilesStorage localFilesStorage,
                               FolioExecutionContext folioExecutionContext) {
    super(remoteFilesStorage, localFilesStorage);
    this.folioExecutionContext = folioExecutionContext;
  }

  @Override
  public ExitStatus afterStepExecution(StepExecution stepExecution) {
    var exitStatus = stepExecution.getExitStatus();
    var localFilesStorage = super.getLocalFilesStorage();
    var remoteFilesStorage = super.getRemoteFilesStorage();

    var tempFilePath = stepExecution.getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    if (localFilesStorage.notExists(tempFilePath)) {
      log.error("Can't find {}.", tempFilePath);
      return ExitStatus.FAILED;
    }
    var fileName = FilenameUtils.getName(tempFilePath);
    var fullFilePath = buildFullFilePath(fileName);

    String uploadedFilePath;
    try {
      uploadedFilePath = remoteFilesStorage.uploadObject(fullFilePath, tempFilePath, null, "text/csv", true);
    } catch (Exception e) {
      log.error(e.toString(), e);
      stepExecution.getJobExecution().addFailureException(e);
      return ExitStatus.FAILED;
    }

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.E_HOLDINGS_FILE_NAME, fileName, ";");
    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, uploadedFilePath, ";");

    return exitStatus;
  }


  private String buildFullFilePath(String fileName) {
    var workDir = getWorkingDirectory(springApplicationName, E_HOLDINGS_EXPORT_DIR_NAME);
    var tenantName = folioExecutionContext.getTenantId();

    return String.format("%s%s/%s", workDir, tenantName, fileName);
  }
}
