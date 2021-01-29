package org.folio.services;

import org.folio.dto.JobParameterDto;
import org.folio.dto.StartJobCommandDto;
import org.folio.model.ExportJobManager;
import org.folio.model.entities.constants.JobParameterNames;
import org.folio.model.repositories.IAcknowledgementRepository;
import org.springframework.batch.core.*;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class JobCommandsReceiverService {

    private static final String DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME = "dataExportJobCommandsTopic";

    private ExportJobManager exportJobManager;

    private Job getGreetingsJob;

    private IAcknowledgementRepository acknowledgementRepository;

    @Autowired
    public JobCommandsReceiverService(
            ExportJobManager exportJobManager,
            Job getGreetingsJob,
            IAcknowledgementRepository acknowledgementRepository) {
        this.exportJobManager = exportJobManager;
        this.getGreetingsJob = getGreetingsJob;
        this.acknowledgementRepository = acknowledgementRepository;
    }

    @KafkaListener(topics = { DATA_EXPORT_JOB_COMMANDS_TOPIC_NAME })
    public void receiveStartJobCommand(StartJobCommandDto startJobCommand, Acknowledgment acknowledgment) throws JobExecutionException {

        String jobId = startJobCommand.getId().toString();
        String outputFilePath = "C:\\Users\\vasily_gancharov\\Desktop\\CSVOutput\\Greetings_" + jobId + ".csv";
        Map<String, JobParameterDto> jobInputParameters = startJobCommand.getJobInputParameters();
        jobInputParameters.put(JobParameterNames.OUTPUT_FILE_PATH, new JobParameterDto(outputFilePath));
        jobInputParameters.put(JobParameterNames.JOB_ID, new JobParameterDto(jobId));
        JobParameters jobParameters = toJobParameters(jobInputParameters);

        JobLaunchRequest jobLaunchRequest = new JobLaunchRequest(this.getGreetingsJob, jobParameters);

        try {
            this.acknowledgementRepository.addAcknowledgement(jobId, acknowledgment);
            JobExecution jobExecution = this.exportJobManager.launchJob(jobLaunchRequest);
        } catch (Exception ex) {
            // TODO log exception here
            ex.printStackTrace();
        }
    }

    private JobParameters toJobParameters(Map<String, JobParameterDto> jobInputParameters) {

        JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

        for (Map.Entry<String, JobParameterDto> inputParameter : jobInputParameters.entrySet()) {
            addJobParameter(jobParametersBuilder, inputParameter.getKey(), inputParameter.getValue());
        }

        return jobParametersBuilder.toJobParameters();
    }

    private JobParameter addJobParameter(
            JobParametersBuilder jobParametersBuilder,
            String jobParameterName,
            JobParameterDto jobParameterDto) {

        Object parameterValue = jobParameterDto.getParameter();
        boolean identifying = jobParameterDto.isIdentifying();

        JobParameter jobParameter = null;
        if (parameterValue.getClass() == Long.class) {
            jobParametersBuilder.addLong(jobParameterName, (Long) parameterValue);
        }
        else if (parameterValue.getClass() == Integer.class) {
            Long value = ((Integer)parameterValue).longValue();
            jobParametersBuilder.addLong(jobParameterName, value);
        }
        else if (parameterValue.getClass() == Double.class) {
            jobParametersBuilder.addDouble(jobParameterName, (Double) parameterValue);
        }
        else if (parameterValue.getClass() == Date.class) {
            jobParametersBuilder.addDate(jobParameterName, (Date) parameterValue);
        }
        else {
            jobParametersBuilder.addString(jobParameterName, (String) parameterValue);
        }

        return jobParameter;
    }
}
