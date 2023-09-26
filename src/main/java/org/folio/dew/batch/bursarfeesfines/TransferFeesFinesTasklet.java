package org.folio.dew.batch.bursarfeesfines;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class TransferFeesFinesTasklet implements Tasklet {

  private final BursarExportService exportService;

  @Override
  public RepeatStatus execute(
    StepContribution contribution,
    ChunkContext chunkContext
  ) {
    // from AccountItemReader
    List<AccountWithAncillaryData> filteredAccounts = (List<AccountWithAncillaryData>) contribution
      .getStepExecution()
      .getJobExecution()
      .getExecutionContext()
      .get("filteredAccounts");

    if (filteredAccounts != null && !filteredAccounts.isEmpty()) {
      exportService.transferAccounts(
        filteredAccounts,
        (BursarExportJob) ExecutionContextUtils.getExecutionVariable(
          contribution.getStepExecution(),
          "jobConfig"
        )
      );
    }

    return RepeatStatus.FINISHED;
  }
}
