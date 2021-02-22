package org.folio.dew.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.entity.constants.JobParameterNames;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

  private final IAcknowledgementRepository acknowledgementRepository;

  @Override
  public void afterJob(JobExecution jobExecution) {
    final String jobCompleteMessageTemplate = "Job finished. Job id: %s. Job Batch id: %s.";

    JobParameters jobParameters = jobExecution.getJobParameters();
    String jobId = jobParameters.getString(JobParameterNames.JOB_ID);
    Long jobBatchId = jobExecution.getJobId();

    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      String jobCompleteMessage = String.format(jobCompleteMessageTemplate, jobId, jobBatchId);
      log.info(jobCompleteMessage);
    }

    Acknowledgment acknowledgment = this.acknowledgementRepository.getAcknowledgement(jobId);
    acknowledgment.acknowledge();
    acknowledgementRepository.deleteAcknowledgement(jobId);
  }

}
