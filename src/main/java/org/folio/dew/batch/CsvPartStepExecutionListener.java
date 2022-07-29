package org.folio.dew.batch;

import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
public class CsvPartStepExecutionListener extends BaseStepListener {

  public CsvPartStepExecutionListener(RemoteFilesStorage remoteFilesStorage, LocalFilesStorage localFilesStorage) {
    super(remoteFilesStorage, localFilesStorage);
  }

  @Override
  public ExitStatus afterStepExecution(StepExecution stepExecution) {
    var exitStatus = stepExecution.getExitStatus();
    var localFilesStorage = super.getLocalFilesStorage();
    var remoteFilesStorage = super.getRemoteFilesStorage();

    var filename = stepExecution.getExecutionContext().getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
    if (localFilesStorage.notExists(filename)) {
      log.error("Can't find {}.", filename);
      return ExitStatus.FAILED;
    }

    try {
      remoteFilesStorage.uploadObject(FilenameUtils.getName(filename), filename, null, "text/csv", true);
    } catch (Exception e) {
      log.error(e.toString(), e);
      stepExecution.getJobExecution().addFailureException(e);
      return ExitStatus.FAILED;
    }

    return exitStatus;
  }

}
