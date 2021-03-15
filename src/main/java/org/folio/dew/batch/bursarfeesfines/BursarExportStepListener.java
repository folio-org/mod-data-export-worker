package org.folio.dew.batch.bursarfeesfines;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.utils.BursarFeesFinesUtils;
import org.folio.dew.utils.ExecutionContextUtils;
import org.folio.dew.utils.JobParameterNames;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class BursarExportStepListener implements StepExecutionListener {

  private final MinIOObjectStorageRepository repository;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    // Nothing to do
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    String downloadFilename = BursarFeesFinesUtils.getFilename(stepExecution.getStepName());
    String filename = stepExecution.getJobExecution()
        .getJobParameters()
        .getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH) + '_' + downloadFilename;

    String url;
    try {
      url = repository.objectWriteResponseToPresignedObjectUrl(
          repository.uploadObject(FilenameUtils.getName(filename), filename, downloadFilename, MediaType.TEXT_MARKDOWN_VALUE));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }

    ExecutionContextUtils.addToJobExecutionContext(stepExecution, JobParameterNames.OUTPUT_FILES_IN_STORAGE, url);

    return stepExecution.getExitStatus();
  }

}
