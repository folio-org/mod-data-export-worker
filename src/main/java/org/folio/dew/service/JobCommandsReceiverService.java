package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.JobParameterNames;
import org.folio.des.domain.dto.StartJobCommand;
import org.folio.des.service.JobExecutionService;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
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
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  private void prepareJobParameters(StartJobCommand startJobCommand) {
    Map<String, JobParameter> parameters = startJobCommand.getJobParameters().getParameters();
    parameters.put(JobParameterNames.JOB_ID, new JobParameter(startJobCommand.getId().toString()));
    parameters.put(JobParameterNames.OUTPUT_FILE_PATH,
        new JobParameter(String.format("/%s/%tF/", startJobCommand.getType(), new Date())));
    startJobCommand.setJobParameters(new JobParameters(parameters));
  }

}
