package org.folio.dew.batch.bursarfeesfines;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@StepScope
@RequiredArgsConstructor
public class TransferFeesFinesTasklet implements Tasklet {

  private final BursarExportService exportService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    BursarExportJob jobConfig = contribution
      .getStepExecution()
      .getJobExecution()
      .getExecutionContext()
      .get("jobConfig", BursarExportJob.class);

    if (Boolean.TRUE.equals(jobConfig.getDryRun())) {
      log.warn("Bursar export is configured as a dry run; no transfer will be performed.");
      return RepeatStatus.FINISHED;
    }

    // supplied from AccountItemReader
    @SuppressWarnings("unchecked")
    List<AccountWithAncillaryData> filteredAccounts = (List<AccountWithAncillaryData>) contribution
      .getStepExecution()
      .getJobExecution()
      .getExecutionContext()
      .get("filteredAccounts");

    if (CollectionUtils.isEmpty(filteredAccounts)) {
      log.warn("No accounts to transfer for this export job; skipping transfer step.");
      return RepeatStatus.FINISHED;
    }

    exportService.transferAccounts(filteredAccounts, jobConfig);

    return RepeatStatus.FINISHED;
  }
}
