package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.batch.acquisitions.edifact.jobs.EdifactExportJobConfig.POL_MEM_KEY;
import static org.folio.dew.domain.dto.JobParameterNames.ACQ_EXPORT_FILE_NAME;
import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.acquisitions.edifact.services.OrganizationsService;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.dew.domain.dto.ExportHistory;
import org.folio.dew.domain.dto.JobParameterNames;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@SuperBuilder
@Component
@StepScope
@Log4j2
public class ExportHistoryTasklet implements Tasklet {

  private final KafkaService kafkaService;
  private final ObjectMapper ediObjectMapper;
  private final OrganizationsService organizationsService;

  @Value("#{jobParameters['jobId']}")
  private String jobId;
  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    var exportHistory = buildExportHistory(chunkContext);
    kafkaService.send(KafkaService.Topic.EXPORT_HISTORY_CREATE, null, exportHistory);

    return RepeatStatus.FINISHED;
  }

  @SneakyThrows
  ExportHistory buildExportHistory(ChunkContext chunkContext) {
    var jobParameters = chunkContext.getStepContext().getJobParameters();
    var ediExportConfig = ediObjectMapper.readValue((String)jobParameters.get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
    var vendorId = ediExportConfig.getVendorId().toString();
    var exportMethod = ediExportConfig.getConfigName();
    var vendor = organizationsService.getOrganizationById(vendorId);
    var vendorName = vendor.get("code").asText();
    var stepExecutionContext = chunkContext.getStepContext().getStepExecution();
    var poLineIds = getPoLineIdsFromExecutionContext(stepExecutionContext);
    var fileName = ExecutionContextUtils.getExecutionVariable(stepExecutionContext, ACQ_EXPORT_FILE_NAME).toString();
    var jobName = jobParameters.get(JobParameterNames.JOB_NAME).toString();

    return new ExportHistory()
      .id(UUID.randomUUID().toString())
      .exportJobId(jobId)
      .exportDate(new Date())
      .exportMethod(exportMethod)
      .exportFileName(fileName)
      .vendorId(vendorId)
      .vendorName(vendorName)
      .exportType("EDIFACT")
      .exportedPoLineIds(poLineIds)
      .jobName(jobName);
  }

  List<String> getPoLineIdsFromExecutionContext(StepExecution stepExecutionContext) {
    try {
      return ediObjectMapper.readValue((String) ExecutionContextUtils.getExecutionVariable(stepExecutionContext, POL_MEM_KEY), new TypeReference<>() {});
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

}
