package org.folio.dew.batch.acquisitions.edifact.jobs;

import static org.folio.dew.domain.dto.JobParameterNames.EDIFACT_ORDERS_EXPORT;

import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Log4j2
@SuperBuilder
public abstract class FilterableTasklet implements Tasklet {

  private final ObjectMapper objectMapper;

  @Override
  public final RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
    var ediExportConfig = objectMapper.readValue((String) chunkContext.getStepContext().getJobParameters().get(EDIFACT_ORDERS_EXPORT), VendorEdiOrdersExportConfig.class);
    if (shouldExecute(ediExportConfig)) {
      return execute(ediExportConfig, contribution, chunkContext);
    }
    return RepeatStatus.FINISHED;
  }

  /**
   * Check if the tasklet should be executed, by default always returns true.
   * Override this method to provide custom logic.
   *
   * @param exportConfig the export configuration
   * @return true if the tasklet should be executed
   */
  protected boolean shouldExecute(VendorEdiOrdersExportConfig exportConfig) {
    return true;
  }


  /**
   * This method should be overridden to provide the tasklet logic with predefined filtering.
   *
   * @param exportConfig the export configuration
   * @param contribution the step contribution
   * @param chunkContext the chunk context
   * @return the repeat status
   * @throws Exception if an error occurs
   */
  protected abstract RepeatStatus execute(VendorEdiOrdersExportConfig exportConfig, StepContribution contribution, ChunkContext chunkContext) throws Exception;

}
