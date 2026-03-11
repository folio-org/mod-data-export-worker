package org.folio.dew.batch;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class ExportJobManager {

  private final JobLaunchingMessageHandler jobLaunchingMessageHandler;
  private final JobRepository jobExplorer;

  @Autowired
  public ExportJobManager(@Qualifier("asyncJobLauncher") JobOperator jobOperator, JobRepository jobExplorer) {
    jobLaunchingMessageHandler = new JobLaunchingMessageHandler(jobOperator);
    this.jobExplorer = jobExplorer;
  }

  public JobExecution launchJob(JobLaunchRequest jobLaunchRequest) throws JobExecutionException {
    log.info("Launching {}.", jobLaunchRequest);
    return jobLaunchingMessageHandler.launch(jobLaunchRequest);
  }

  public JobExecution getJobExecution(Long jobId) {
    return jobExplorer.getJobExecution(jobId);
  }

}
