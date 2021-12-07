package org.folio.dew.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FilenameUtils;
import org.folio.des.domain.JobParameterNames;
import org.folio.dew.domain.dto.ItemIdentifier;
import org.folio.dew.service.SaveErrorService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.annotation.OnSkipInProcess;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@JobScope
@Component
public class BulkEditSkipListener {

  private final SaveErrorService saveErrorService;

  @Value("#{jobExecution}")
  private JobExecution jobExecution;

  @OnSkipInProcess
  public void onSkipInProcess(ItemIdentifier itemIdentifier, BulkEditException bulkEditException) {
    log.debug(bulkEditException);
    saveErrorService.saveErrorInCSV(jobExecution.getJobParameters().getString(JobParameterNames.JOB_ID), itemIdentifier.getItemId(), bulkEditException, FilenameUtils.getName(jobExecution.getJobParameters().getString("identifiersFileName")));
  }

}
