package org.folio.model;

import org.apache.commons.io.FilenameUtils;
import org.folio.model.entities.constants.JobParameterNames;
import org.folio.model.repositories.MinIOObjectStorageRepository;
import org.folio.utils.ExecutionContextUtils;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class CsvFileAssembler implements StepExecutionAggregator {

    private MinIOObjectStorageRepository minIOObjectStorageRepository;

    private String workspaceBucketName;

    @Autowired
    public CsvFileAssembler(
            MinIOObjectStorageRepository minIOObjectStorageRepository,
            @Value("${minio.workspaceBucketName}") String workspaceBucketName) {
        this.minIOObjectStorageRepository = minIOObjectStorageRepository;
        this.workspaceBucketName = workspaceBucketName;
    }

    @Override
    public void aggregate(StepExecution stepExecution, Collection<StepExecution> finishedStepExecutions) {

        List<String> csvFilePartObjectNames = new ArrayList<>();
        for (StepExecution currentFinishedStepExecution : finishedStepExecutions) {
            ExecutionContext executionContext = currentFinishedStepExecution.getExecutionContext();
            String objectName = ExecutionContextUtils.getObjectNameByOutputFilePath(executionContext);
            csvFilePartObjectNames.add(objectName);
        }

        // TODO if there is a single object name in csvFilePartObjectNames, just rename it (remove .tmp) instead of assembling target CSV file

        JobParameters jobParameters = stepExecution.getJobExecution().getJobParameters();
        String outputFilePath = jobParameters.getString(JobParameterNames.OUTPUT_FILE_PATH);
        String csvObjectName = FilenameUtils.getName(outputFilePath);

        try {
            this.minIOObjectStorageRepository.composeObject(workspaceBucketName, csvObjectName, csvFilePartObjectNames);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // TODO delete all local .csv.tmp files related to the job here

        // TODO delete all .csv.tmp objects from MinIO related to the job here
    }
}
