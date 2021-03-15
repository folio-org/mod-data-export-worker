package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.StartJobCommand;
import org.folio.des.service.JobExecutionService;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.utils.JobParameterNames;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService {

  private final ExportJobManager exportJobManager;
  private final IAcknowledgementRepository acknowledgementRepository;
  private final ApplicationContext applicationContext;
  private final List<Job> jobs;
  private Map<String, Job> jobMap;
  @Value("${spring.application.name}")
  private String springApplicationName;
  private String workDir;

  @PostConstruct
  public void postConstruct() {
    jobMap = new LinkedHashMap<>();
    for (Job job : jobs) {
      jobMap.put(job.getName(), job);
    }

    workDir = System.getProperty("java.io.tmpdir") + '/' + springApplicationName + '/';
    File file = new File(workDir);
    if (!file.exists()) {
      if (file.mkdir()) {
        log.info("Created working directory {}.", workDir);
      } else {
        log.fatal("Can't create working directory {}.", workDir);
        SpringApplication.exit(applicationContext, () -> 1);
      }
    } else {
      log.info("Working directory {}.", workDir);
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
    String jobId = startJobCommand.getId().toString();
    parameters.put(JobParameterNames.JOB_ID, new JobParameter(jobId));
    Date now = new Date();
    parameters.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH,
        new JobParameter(String.format("%s%s_%tF_%tT_%s", workDir, startJobCommand.getType(), now, now, jobId)));
    startJobCommand.setJobParameters(new JobParameters(parameters));
  }

}
