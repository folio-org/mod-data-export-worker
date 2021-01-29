package org.folio.model;

import org.folio.model.entities.constants.JobParameterNames;
import org.folio.model.repositories.IAcknowledgementRepository;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    private IAcknowledgementRepository acknowledgementRepository;

    @Autowired
    public JobCompletionNotificationListener(IAcknowledgementRepository acknowledgementRepository) {
        this.acknowledgementRepository = acknowledgementRepository;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        final String jobCompleteMessageTemplate = "Job finished. Job id: %s. Job Batch id: %s.";

        JobParameters jobParameters = jobExecution.getJobParameters();
        String jobId = jobParameters.getString(JobParameterNames.JOB_ID);
        Long jobBatchId = jobExecution.getJobId();

        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            String jobCompleteMessage = String.format(jobCompleteMessageTemplate, jobId, jobBatchId);
            System.out.println(jobCompleteMessage);
        }

        Acknowledgment acknowledgment = this.acknowledgementRepository.getAcknowledgement(jobId);
        acknowledgment.acknowledge();
        this.acknowledgementRepository.deleteAcknowledgement(jobId);
    }
}

