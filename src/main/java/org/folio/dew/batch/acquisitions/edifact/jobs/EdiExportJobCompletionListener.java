package org.folio.dew.batch.acquisitions.edifact.jobs;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.de.entity.Job;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
@RequiredArgsConstructor
public class EdiExportJobCompletionListener extends JobExecutionListenerSupport {

  private static final String PATHS_DELIMITER = ";";

  private final IAcknowledgementRepository acknowledgementRepository;
  private final KafkaService kafka;


  @Override
  public void beforeJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, false);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    processJobUpdate(jobExecution, true);
  }

  @SneakyThrows
  private void processJobUpdate(JobExecution jobExecution, boolean after) {
    var jobParameters = jobExecution.getJobParameters();
    var jobId = jobParameters.getString(JobParameterNames.JOB_ID);
    if (StringUtils.isBlank(jobId)) {
      log.error("Job update with empty Job ID {}.", jobExecution);
      return;
    }
    log.info("Job update {}.", jobExecution);

    var jobExecutionUpdate = createJobExecutionUpdate(jobId, jobExecution);

    var acknowledgment = acknowledgementRepository.getAcknowledgement(jobId);
    if (acknowledgment != null) {
      acknowledgment.acknowledge();
      acknowledgementRepository.deleteAcknowledgement(jobId);
    }

    kafka.send(KafkaService.Topic.JOB_UPDATE, jobExecutionUpdate.getId().toString(), jobExecutionUpdate);
    if (after) {
      log.info("-----------------------------JOB---ENDS-----------------------------");
    }
  }

  private Job createJobExecutionUpdate(String jobId, JobExecution jobExecution) {
    Job result = new Job();

    result.setId(UUID.fromString(jobId));

    String jobDescription = ExecutionContextUtils.getFromJobExecutionContext(jobExecution, JobParameterNames.JOB_DESCRIPTION);
    if (StringUtils.isNotBlank(jobDescription)) {
      result.setDescription(jobDescription);
    }

    String ftpUploadedFile = ExecutionContextUtils.getFromJobExecutionContext(jobExecution,"edifactFileName");
    if (StringUtils.isNotBlank(ftpUploadedFile)) {
      result.setFileNames(List.of(ftpUploadedFile));
    }

    result.setStartTime(jobExecution.getStartTime());
    result.setCreatedDate(jobExecution.getCreateTime());
    result.setEndTime(jobExecution.getEndTime());
    result.setUpdatedDate(jobExecution.getLastUpdated());

    List<Throwable> errors = jobExecution.getAllFailureExceptions();
    if (CollectionUtils.isNotEmpty(errors)) {
      result.setErrorDetails(errors.stream()
        .map(t -> {
          t = getThrowableRootCause(t);
          return t.getMessage() + " (" + t.getClass().getSimpleName() + ')';
        })
        .collect(Collectors.joining("\n")));
    }

    result.setBatchStatus(jobExecution.getStatus());
    result.setExitStatus(jobExecution.getExitStatus());

    return result;
  }

  private Throwable getThrowableRootCause(Throwable t) {
    Throwable cause = t.getCause();
    while (cause != null && cause != t) {
      t = cause;
      cause = t.getCause();
    }
    return t;
  }

}
