package org.folio.dew.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.service.BulkEditProcessingErrorsService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.annotation.OnSkipInProcess;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static org.folio.dew.utils.Constants.FILE_NAME;

@Log4j2
@RequiredArgsConstructor
@StepScope
@Component
public class BulkEditSkipListener {

  private final BulkEditProcessingErrorsService bulkEditProcessingErrorsService;
  @Value("#{stepExecution.jobExecution}")
  private JobExecution jobExecution;

  @OnSkipInProcess
  public void onSkipInProcess(ItemIdentifier itemIdentifier, Throwable exception) {
    log.debug(exception);
    if (exception instanceof BulkEditException) {
      bulkEditProcessingErrorsService.saveErrorInCSV(jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID), itemIdentifier.getItemId(), (BulkEditException) exception, FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
    }
    if (exception instanceof BulkEditMultiException) {
      bulkEditProcessingErrorsService.saveErrorInCSV(jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID), itemIdentifier.getItemId(), (BulkEditMultiException) exception, FilenameUtils.getName(jobExecution.getJobParameters().getString(FILE_NAME)));
    }
  }
}
