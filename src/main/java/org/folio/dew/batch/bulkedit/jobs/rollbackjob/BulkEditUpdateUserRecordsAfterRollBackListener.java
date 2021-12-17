package org.folio.dew.batch.bulkedit.jobs.rollbackjob;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.folio.dew.service.BulkEditRollBackService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.UUID;

@Component
@JobScope
@RequiredArgsConstructor
public class BulkEditUpdateUserRecordsAfterRollBackListener implements JobExecutionListener {

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Value("#{jobParameters['fileName']}")
  private String fileName;
  private final BulkEditRollBackService bulkEditRollBackService;

  @Override
  public void beforeJob(JobExecution jobExecution) {}

  @Override
  public void afterJob(JobExecution jobExecution) {
    bulkEditRollBackService.cleanJobData(UUID.fromString(jobId));
    FileUtils.deleteQuietly(new File(fileName));
  }
}
