package org.folio.dew.batch.acquisitions.edifact.jobs.decider;

import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;

import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public abstract class ExportStepDecider implements JobExecutionDecider {

  private final ObjectMapper objectMapper;
  protected final String stepName;

  @Override
  @SneakyThrows
  public final FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
    var exportConfig = objectMapper.readValue(jobExecution.getJobParameters().getString(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
    return new FlowExecutionStatus(decide(exportConfig, jobExecution, stepExecution).getStatus());
  }

  /**
   * Evaluate the decision to execute the step or not using the provided export configuration, job execution and step
   *
   * @param exportConfig export configuration
   * @param jobExecution job execution
   * @param stepExecution step execution
   * @return the decision to execute the step or not
   * @throws Exception if an error occurs during the decision process
   */
  protected abstract ExportStepDecision decide(VendorEdiOrdersExportConfig exportConfig, JobExecution jobExecution, StepExecution stepExecution) throws Exception;

}
