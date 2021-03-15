package org.folio.dew.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.entity.Job;
import org.folio.des.service.JobUpdatesService;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.utils.JobParameterNames;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
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

      String tempOutputFilePath = jobParameters.getString(JobParameterNames.TEMP_OUTPUT_FILE_PATH);
      if (StringUtils.isNotBlank(tempOutputFilePath)) {
        String path = FilenameUtils.getFullPath(tempOutputFilePath);
        String fileNameStart = FilenameUtils.getName(tempOutputFilePath);
        if (StringUtils.isNotBlank(path) && StringUtils.isNotBlank(fileNameStart)) {
          File[] files = new File(path).listFiles((dir, name) -> name.startsWith(fileNameStart));
          if (files != null && files.length > 0) {
            for (File f : files) {
              f.delete();
            }
            log.info("Job {} temp files {} deleted.", jobId, files);
          }
        }
      }
    }

    Job jobExecutionUpdate = new Job();
    jobExecutionUpdate.setId(UUID.fromString(jobId));
    String outputFilesInStorage = jobExecution.getExecutionContext().getString(JobParameterNames.OUTPUT_FILES_IN_STORAGE);
    if (StringUtils.isNotBlank(outputFilesInStorage)) {
      jobExecutionUpdate.setFiles(Arrays.asList(outputFilesInStorage.split(";")));
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
    while (cause != null && cause != t) {
      t = cause;
      cause = t.getCause();
    }
    return t;
  }

}
