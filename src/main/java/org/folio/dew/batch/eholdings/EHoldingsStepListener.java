package org.folio.dew.batch.eholdings;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.BaseStepListener;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;


@Log4j2
@Component
public class EHoldingsStepListener extends BaseStepListener {

  public EHoldingsStepListener(RemoteFilesStorage remoteFilesStorage, LocalFilesStorage localFilesStorage) {
    super(remoteFilesStorage, localFilesStorage);
  }

  @Override
  public ExitStatus afterStepExecution(StepExecution stepExecution) {
    var exitStatus = stepExecution.getExitStatus();
    var localFilesStorage = super.getLocalFilesStorage();
    var remoteFilesStorage = super.getRemoteFilesStorage();

    var filename = stepExecution.getJobParameters().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    if (localFilesStorage.notExists(filename)) {
      log.error("Can't find {}.", filename);
      return ExitStatus.FAILED;
    }

    String uploadedFilePath;
    try {
      uploadedFilePath = remoteFilesStorage.uploadObject(FilenameUtils.getName(filename), filename, null, "text/csv", true);
    } catch (Exception e) {
      log.error(e.toString(), e);
      stepExecution.getJobExecution().addFailureException(e);
      return ExitStatus.FAILED;
    }

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, uploadedFilePath, ";");

    return exitStatus;
  }
}
