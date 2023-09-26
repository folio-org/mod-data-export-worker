package org.folio.dew.batch.bursarfeesfines;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.BaseStepListener;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportUtils;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class BursarExportStepListener extends BaseStepListener {

  public BursarExportStepListener(
    RemoteFilesStorage remoteFilesStorage,
    LocalFilesStorage localFilesStorage
  ) {
    super(remoteFilesStorage, localFilesStorage);
  }

  @Override
  public ExitStatus afterStepExecution(StepExecution stepExecution) {
    // this method should only apply for the main export step
    if (
      stepExecution.getStepName().equals(BursarExportUtils.GET_FILENAME_STEP)
    ) {
      return stepExecution.getExitStatus();
    }

    var exitStatus = stepExecution.getExitStatus();
    var localFilesStorage = super.getLocalFilesStorage();
    var remoteFilesStorage = super.getRemoteFilesStorage();

    var jobExecution = stepExecution.getJobExecution();
    String downloadFilename = jobExecution
      .getExecutionContext()
      .getString("filename");
    String filename =
      jobExecution
        .getJobParameters()
        .getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) +
      '_' +
      downloadFilename;
    if (localFilesStorage.notExists(filename)) {
      log.error("Can't find {}.", filename);
      return ExitStatus.FAILED;
    }

    String url;
    try {
      url =
        remoteFilesStorage.objectToPresignedObjectUrl(
          remoteFilesStorage.uploadObject(
            FilenameUtils.getName(filename),
            filename,
            downloadFilename,
            MediaType.TEXT_MARKDOWN_VALUE,
            true
          )
        );
    } catch (Exception e) {
      log.error(e.toString(), e);
      jobExecution.addFailureException(e);
      return ExitStatus.FAILED;
    }

    ExecutionContextUtils.addToJobExecutionContext(
      stepExecution,
      JobParameterNames.OUTPUT_FILES_IN_STORAGE,
      url,
      ";"
    );

    ExecutionContextUtils.addToJobExecutionContext(
      stepExecution,
      JobParameterNames.JOB_DESCRIPTION,
      String.format(
        BursarExportUtils.getJobDescriptionPart(),
        stepExecution.getWriteCount()
      ),
      "\n"
    );

    return exitStatus;
  }
}
