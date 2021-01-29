package org.folio.model;

import io.minio.ObjectWriteResponse;
import org.apache.commons.io.FilenameUtils;
import org.folio.model.entities.constants.JobParameterNames;
import org.folio.model.repositories.MinIOObjectStorageRepository;
import org.folio.utils.ExecutionContextUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GreetingsPartStepExecutionListener implements StepExecutionListener {

    private MinIOObjectStorageRepository objectStorageRepository;

    private String workspaceBucketName;

    @Autowired
    public GreetingsPartStepExecutionListener(
            MinIOObjectStorageRepository objectStorageRepository,
            @Value("${minio.workspaceBucketName}") String workspaceBucketName) {
        this.objectStorageRepository = objectStorageRepository;
        this.workspaceBucketName = workspaceBucketName;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        final String csvFilePartContentType = "csv";

        ExecutionContext executionContext = stepExecution.getExecutionContext();
        String outputFilePath = executionContext.getString(JobParameterNames.OUTPUT_FILE_PATH);
        String objectName = ExecutionContextUtils.getObjectNameByOutputFilePath(executionContext);

        try {
            ObjectWriteResponse objectWriteResponse = this.objectStorageRepository.uploadObject(this.workspaceBucketName, objectName, outputFilePath, csvFilePartContentType);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return stepExecution.getExitStatus();
    }
}
