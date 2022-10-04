package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;
import static org.folio.dew.domain.dto.JobParameterNames.OUTPUT_FILES_IN_STORAGE;
import static org.folio.dew.domain.dto.JobParameterNames.UPLOADED_FILE_PATH;
import static org.folio.dew.utils.Constants.EDIFACT_EXPORT_DIR_NAME;
import static org.folio.dew.utils.Constants.getWorkingDirectory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.FilenameUtils;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.repository.LocalFilesStorage;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.FolioExecutionContext;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Component
@StepScope
@Log4j2
public class SaveToMinioTasklet implements Tasklet {
  private final RemoteFilesStorage remoteFilesStorage;
  private final LocalFilesStorage localFilesStorage;
  private final OrganizationsService organizationsService;
  private final FolioExecutionContext folioExecutionContext;
  private final ObjectMapper objectMapper;
  private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

  @Value("${spring.application.name}")
  protected String springApplicationName;
  @Value("#{jobParameters['edifactOrderAsString']}")
  private String edifactOrderAsString;

  @Override
  @SneakyThrows
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    // retrieve parameters from job context
    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = objectMapper.readValue((String)jobParameters.get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);

    var uploadedLocalFile = createTempFile(ediExportConfig, edifactOrderAsString);
    String edifactFileName = FilenameUtils.getName(uploadedLocalFile);
    var pathToUpload = folioExecutionContext.getTenantId() + edifactFileName;

    var uploadedFilePath = remoteFilesStorage.objectToPresignedObjectUrl(
      remoteFilesStorage.uploadObject(pathToUpload, uploadedLocalFile, uploadedLocalFile, MediaType.TEXT_PLAIN_VALUE, false));

    ExecutionContextUtils.addToJobExecutionContext(contribution.getStepExecution(), UPLOADED_FILE_PATH, uploadedLocalFile, "");
    ExecutionContextUtils.addToJobExecutionContext(contribution.getStepExecution(), EDIFACT_FILE_NAME, edifactFileName, "");
    ExecutionContextUtils.addToJobExecutionContext(contribution.getStepExecution(), OUTPUT_FILES_IN_STORAGE, uploadedFilePath, ";");

    return RepeatStatus.FINISHED;
  }

  private String generateFileName(VendorEdiOrdersExportConfig ediExportConfig) {
    var vendorId = ediExportConfig.getVendorId().toString();
    var vendor = organizationsService.getOrganizationById(vendorId);
    var vendorName = vendor.get("code").asText();
    var fileDate = dateFormat.format(new Date());
    // exclude restricted symbols after implementing naming convention feature
    return vendorName + "_" + ediExportConfig.getConfigName() + "_" + fileDate + ".edi";
  }

  private String createTempFile(VendorEdiOrdersExportConfig ediExportConfig, String content) throws IOException {
    String workDir = getWorkingDirectory(springApplicationName, EDIFACT_EXPORT_DIR_NAME);
    var filename = generateFileName(ediExportConfig);

    return localFilesStorage.write(workDir + filename, content.getBytes(StandardCharsets.UTF_8));
  }
}
