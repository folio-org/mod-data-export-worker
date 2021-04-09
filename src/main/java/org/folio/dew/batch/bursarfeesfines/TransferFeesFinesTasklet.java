package org.folio.dew.batch.bursarfeesfines;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.dew.batch.ExecutionContextUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.Account;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
@StepScope
public class TransferFeesFinesTasklet implements Tasklet {

  private final BursarExportService exportService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    List<Account> accounts = (List<Account>) ExecutionContextUtils.getExecutionVariable(contribution.getStepExecution(),
        "accounts");
    if (CollectionUtils.isNotEmpty(accounts)) {
      exportService.transferAccounts(accounts,
          (BursarFeeFines) ExecutionContextUtils.getExecutionVariable(contribution.getStepExecution(), "bursarFeeFines"));
    }
    return RepeatStatus.FINISHED;
  }

}
