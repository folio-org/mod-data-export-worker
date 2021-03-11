package org.folio.dew.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.JobParameterNames;
import org.folio.des.domain.entity.Job;
import org.folio.des.service.JobUpdatesService;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Log4j2
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

  private final IAcknowledgementRepository acknowledgementRepository;
  private final KafkaTemplate<String, Job> kafkaTemplate;

  @Override
  public void beforeJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, false);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, true);
  }

  private void processJobUpdate(JobExecution jobExecution, boolean after) {
    JobParameters jobParameters = jobExecution.getJobParameters();
    String jobId = jobParameters.getString(JobParameterNames.JOB_ID);
    if (StringUtils.isBlank(jobId)) {
      log.error("Job update with empty Job ID {}.", jobExecution);
      return;
    }

    log.info("Job update {}.", jobExecution);

    if (after) {
      Acknowledgment acknowledgment = acknowledgementRepository.getAcknowledgement(jobId);
      if (acknowledgment != null) {
        acknowledgment.acknowledge();
        acknowledgementRepository.deleteAcknowledgement(jobId);
      }
    }

    Job jobExecutionUpdate = new Job();
    jobExecutionUpdate.setId(UUID.fromString(jobId));
    String outputFile = jobParameters.getString(JobParameterNames.OUTPUT_FILE_PATH);
    if (StringUtils.isNotBlank(outputFile)) {
      jobExecutionUpdate.setFiles(Collections.singletonList(outputFile));
    }
    jobExecutionUpdate.setStartTime(jobExecution.getStartTime());
    jobExecutionUpdate.setCreatedDate(jobExecution.getCreateTime());
    jobExecutionUpdate.setEndTime(jobExecution.getEndTime());
    jobExecutionUpdate.setUpdatedDate(jobExecution.getLastUpdated());
    List<Throwable> errors = jobExecution.getAllFailureExceptions();
    if (CollectionUtils.isNotEmpty(errors)) {
      jobExecutionUpdate.setErrorDetails(
          errors.stream().map(t -> getThrowableRootCause(t).getMessage()).collect(Collectors.joining("\n")));
    }
    jobExecutionUpdate.setBatchStatus(jobExecution.getStatus());
    jobExecutionUpdate.setExitStatus(jobExecution.getExitStatus());
    log.info("Sending {}.", jobExecutionUpdate);
    kafkaTemplate.send(JobUpdatesService.DATA_EXPORT_JOB_EXECUTION_UPDATES_TOPIC_NAME, jobExecutionUpdate.getId().toString(),
        jobExecutionUpdate);
    log.info("Sent job {} update.", jobExecutionUpdate.getId());
  }

  private Throwable getThrowableRootCause(Throwable t) {
    Throwable cause = t.getCause();
    while (cause != t) {
      t = cause;
      cause = t.getCause();
    }
    return t;
  }

}
