package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobParameterNames;
import org.folio.des.domain.dto.StartJobCommand;
import org.folio.des.service.JobExecutionService;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService {

  private final ExportJobManager exportJobManager;
  private final List<Job> jobs;
  private final IAcknowledgementRepository acknowledgementRepository;
  private Map<String, Job> jobMap;

  @PostConstruct
  public void prepareJobs() {
    jobMap = new LinkedHashMap<>();
    for (Job job : jobs) {
      jobMap.put(job.getName(), job);
    }
  }

  @KafkaListener(topics = { JobExecutionService.DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME })
  public void receiveStartJobCommand(StartJobCommand startJobCommand, Acknowledgment acknowledgment) {
    log.info("Received {}.", startJobCommand);

    prepareJobParameters(startJobCommand);

    JobLaunchRequest jobLaunchRequest = new JobLaunchRequest(jobMap.get(startJobCommand.getType().toString()),
        startJobCommand.getJobParameters());

    try {
      acknowledgementRepository.addAcknowledgement(startJobCommand.getId().toString(), acknowledgment);
      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private void prepareJobParameters(StartJobCommand startJobCommand) {
    String jobId = startJobCommand.getId().toString();
    ExportType exportType = startJobCommand.getType();
    String outputFilePath = "";
    if (exportType == ExportType.CIRCULATION_LOG) {
      outputFilePath = "\\minio\\" + jobId + ".csv";
    } else if (exportType == ExportType.BURSAR_FEES_FINES) {
      outputFilePath = "\\minio\\";
    }

    startJobCommand.getJobParameters().getParameters().put(JobParameterNames.OUTPUT_FILE_PATH, new JobParameter(outputFilePath));
    startJobCommand.getJobParameters().getParameters().put(JobParameterNames.JOB_ID, new JobParameter(jobId));
  }

}
