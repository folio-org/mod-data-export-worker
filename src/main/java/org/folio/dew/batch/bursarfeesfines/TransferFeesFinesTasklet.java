package org.folio.dew.batch.bursarfeesfines;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.service.BursarExportService;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@StepScope
public class TransferFeesFinesTasklet implements Tasklet {
  private final BursarExportService exportService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    ExecutionContext context =
        contribution.getStepExecution().getJobExecution().getExecutionContext();

    List<Account> accounts = (List<Account>) context.get("accounts");
    if (CollectionUtils.isNotEmpty(accounts)) {
      exportService.transferAccounts(accounts);
    }
    return RepeatStatus.FINISHED;
  }
}
