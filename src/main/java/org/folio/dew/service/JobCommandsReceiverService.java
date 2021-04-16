package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.folio.des.config.KafkaConfiguration;
import org.folio.des.domain.JobParameterNames;
import org.folio.des.domain.dto.JobCommand;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService implements AcknowledgingMessageListener<String, JobCommand> {

  private final KafkaConfiguration kafka;
  private final ExportJobManager exportJobManager;
  private final IAcknowledgementRepository acknowledgementRepository;
  private final MinIOObjectStorageRepository objectStorageRepository;
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
        throw new IllegalStateException(String.format("Can't create working directory %s.", workDir));
      }
    } else {
      log.info("Working directory {}.", workDir);
    }
  }

  @EventListener(ContextRefreshedEvent.class)
  public void onContextRefreshed() {
    kafka.startListener(KafkaConfiguration.Topic.JOB_COMMAND, this);
  }

  @Override
  public void onMessage(ConsumerRecord<String, JobCommand> data, Acknowledgment acknowledgment) {
    JobCommand jobCommand = data.value();
    log.info("Received {}.", jobCommand);

    try {
      if (deleteOldFiles(jobCommand, acknowledgment)) {
        return;
      }

      log.info("-----------------------------JOB---STARTS-----------------------------");

      prepareJobParameters(jobCommand);

      JobLaunchRequest jobLaunchRequest = new JobLaunchRequest(jobMap.get(jobCommand.getExportType().toString()),
          jobCommand.getJobParameters());

      acknowledgementRepository.addAcknowledgement(jobCommand.getId().toString(), acknowledgment);
      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception e) {
      log.error(e.toString(), e);
    }
  }

  private void prepareJobParameters(JobCommand jobCommand) {
    Map<String, JobParameter> parameters = jobCommand.getJobParameters().getParameters();
    String jobId = jobCommand.getId().toString();
    parameters.put(JobParameterNames.JOB_ID, new JobParameter(jobId));
    Date now = new Date();
    parameters.put(JobParameterNames.TEMP_OUTPUT_FILE_PATH,
        new JobParameter(String.format("%s%s_%tF_%tT_%s", workDir, jobCommand.getExportType(), now, now, jobId)));
    jobCommand.setJobParameters(new JobParameters(parameters));
  }

  private boolean deleteOldFiles(JobCommand jobCommand, Acknowledgment acknowledgment) {
    if (jobCommand.getType() != JobCommand.Type.DELETE) {
      return false;
    }

    acknowledgment.acknowledge();

    String filesStr = jobCommand.getJobParameters().getString(JobParameterNames.OUTPUT_FILES_IN_STORAGE);
    log.info("Deleting old job files {}.", filesStr);
    if (StringUtils.isEmpty(filesStr)) {
      return true;
    }

    List<String> objects = Arrays.stream(filesStr.split(";")).distinct().map(f -> {
      try {
        return StringUtils.stripStart(new URL(f).getPath(), "/");
      } catch (MalformedURLException e) {
        log.error(e.getMessage(), e);
        return null;
      }
    }).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
    if (!objects.isEmpty()) {
      objectStorageRepository.removeObjects(objects);
    }

    return true;
  }

}
