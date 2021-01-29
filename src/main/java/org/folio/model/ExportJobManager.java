package org.folio.model;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.integration.launch.JobLaunchRequest;
import org.springframework.batch.integration.launch.JobLaunchingMessageHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ExportJobManager {

    private JobLauncher jobLauncher;

    private JobLaunchingMessageHandler jobLaunchingMessageHandler;

    private JobExplorer jobExplorer;

    @Autowired
    public ExportJobManager(@Qualifier("asyncJobLauncher") JobLauncher jobLauncher, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.jobLaunchingMessageHandler = new JobLaunchingMessageHandler(this.jobLauncher);
        this.jobExplorer = jobExplorer;
    }

    public JobExecution launchJob(JobLaunchRequest jobLaunchRequest) throws JobExecutionException {
        // TODO validate input parameters there

        JobExecution jobExecution = this.jobLaunchingMessageHandler.launch(jobLaunchRequest);
        return jobExecution;
    }

    public JobExecution getJobExecution(Long jobId) {
        // TODO validate input parameters there

        JobExecution jobExecution = this.jobExplorer.getJobExecution(jobId);
        return jobExecution;
    }
}
