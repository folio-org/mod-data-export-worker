package org.folio.dew.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.JobParameterDto;
import org.folio.dew.domain.dto.StartJobCommandDto;
import org.folio.dew.domain.entity.constants.JobParameterNames;
import org.folio.dew.model.ExportJobManager;
import org.folio.dew.repository.IAcknowledgementRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class JobCommandsReceiverService {

  private static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

  private final ExportJobManager exportJobManager;
  private final Job job;
  private final IAcknowledgementRepository acknowledgementRepository;

  @KafkaListener(topics = { DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME })
  public void receiveStartJobCommand(StartJobCommandDto startJobCommand, Acknowledgment acknowledgment) {

    String jobId = startJobCommand.getId().toString();
    String outputFilePath = "\\minio\\" + jobId + ".csv";
    Map<String, JobParameterDto> jobInputParameters = startJobCommand.getJobInputParameters();
    jobInputParameters.put(JobParameterNames.OUTPUT_FILE_PATH, new JobParameterDto(outputFilePath));
    jobInputParameters.put(JobParameterNames.JOB_ID, new JobParameterDto(jobId));
    JobParameters jobParameters = toJobParameters(jobInputParameters);

    JobLaunchRequest jobLaunchRequest = new JobLaunchRequest(job, jobParameters);

    try {
      acknowledgementRepository.addAcknowledgement(jobId, acknowledgment);
      exportJobManager.launchJob(jobLaunchRequest);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
    }
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
