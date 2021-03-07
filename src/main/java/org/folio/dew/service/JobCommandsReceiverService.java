package org.folio.dew.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.ExportType;
import org.folio.des.domain.dto.JobParameterDto;
import org.folio.des.domain.dto.StartJobCommandDto;
import org.folio.des.domain.entity.constant.JobParameterNames;
import org.folio.des.service.JobExecutionService;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
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
  public void receiveStartJobCommand(StartJobCommandDto startJobCommand, Acknowledgment acknowledgment) {
    log.info("Received {}.", startJobCommand);

    String jobId = startJobCommand.getId().toString();
    ExportType exportType = startJobCommand.getType();
    Map<String, JobParameterDto> jobInputParameters = prepareJobParameters(startJobCommand);
    JobParameters jobParameters = toJobParameters(jobInputParameters);

    Job job = jobMap.get(exportType.toString());
    JobLaunchRequest jobLaunchRequest = new JobLaunchRequest(job, jobParameters);

    try {
      acknowledgementRepository.addAcknowledgement(jobId, acknowledgment);
      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private Map<String, JobParameterDto> prepareJobParameters(StartJobCommandDto startJobCommand) {
    ExportType exportType = startJobCommand.getType();
    String jobId = startJobCommand.getId().toString();

    Map<String, JobParameterDto> jobInputParameters = startJobCommand.getJobInputParameters();
    String outputFilePath = "";

    if (exportType == ExportType.CIRCULATION_LOG) {
      outputFilePath = "\\minio\\" + jobId + ".csv";
    } else if (exportType == ExportType.BURSAR_FEES_FINES) {
      outputFilePath = "\\minio\\";
    }

    jobInputParameters.put(JobParameterNames.OUTPUT_FILE_PATH, new JobParameterDto(outputFilePath));
    jobInputParameters.put(JobParameterNames.JOB_ID, new JobParameterDto(jobId));

    return jobInputParameters;
  }

  private JobParameters toJobParameters(Map<String, JobParameterDto> jobInputParameters) {
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

    for (Map.Entry<String, JobParameterDto> inputParameter : jobInputParameters.entrySet()) {
      addJobParameter(jobParametersBuilder, inputParameter.getKey(), inputParameter.getValue());
    }

    return jobParametersBuilder.toJobParameters();
  }

  private void addJobParameter(JobParametersBuilder jobParametersBuilder, String jobParameterName,
      JobParameterDto jobParameterDto) {
    Object parameterValue = jobParameterDto.getParameter();

    if (parameterValue.getClass() == Long.class) {
      jobParametersBuilder.addLong(jobParameterName, (Long) parameterValue);
    } else if (parameterValue.getClass() == Integer.class) {
      Long value = ((Integer) parameterValue).longValue();
      jobParametersBuilder.addLong(jobParameterName, value);
    } else if (parameterValue.getClass() == Double.class) {
      jobParametersBuilder.addDouble(jobParameterName, (Double) parameterValue);
    } else if (parameterValue.getClass() == Date.class) {
      jobParametersBuilder.addDate(jobParameterName, (Date) parameterValue);
    } else {
      jobParametersBuilder.addString(jobParameterName, (String) parameterValue);
    }
  }

}
