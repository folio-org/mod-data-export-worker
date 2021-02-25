package org.folio.dew.service;

import static org.folio.des.domain.entity.enums.JobType.BURSAR_FEES_FINES_EXPORT;
import static org.folio.des.domain.entity.enums.JobType.CIRCULATION_LOG_EXPORT;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.des.domain.dto.JobParameterDto;
import org.folio.des.domain.dto.StartJobCommandDto;
import org.folio.dew.batch.ExportJobManager;
import org.folio.des.domain.entity.constant.JobParameterNames;
import org.folio.des.domain.entity.enums.JobType;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService {
  private static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

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

  @KafkaListener(topics = {DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME})
  public void receiveStartJobCommand(
      StartJobCommandDto startJobCommand, Acknowledgment acknowledgment) {

    String jobId = startJobCommand.getId().toString();
    JobType jobType = startJobCommand.getJobType();
    Map<String, JobParameterDto> jobInputParameters = prepareJobParameters(startJobCommand);
    JobParameters jobParameters = toJobParameters(jobInputParameters);

    Job job = jobMap.get(jobType.toString());
    JobLaunchRequest jobLaunchRequest = new JobLaunchRequest(job, jobParameters);

    try {
      acknowledgementRepository.addAcknowledgement(jobId, acknowledgment);
      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
  }

  private Map<String, JobParameterDto> prepareJobParameters(StartJobCommandDto startJobCommand) {
    JobType jobType = startJobCommand.getJobType();
    String jobId = startJobCommand.getId().toString();

    Map<String, JobParameterDto> jobInputParameters = startJobCommand.getJobInputParameters();
    String outputFilePath = "";

    if (jobType == CIRCULATION_LOG_EXPORT) {
      outputFilePath = "\\minio\\" + jobId + ".csv";
    } else if (jobType == BURSAR_FEES_FINES_EXPORT) {
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

  private void addJobParameter(
      JobParametersBuilder jobParametersBuilder,
      String jobParameterName,
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
