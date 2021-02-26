package org.folio.dew.batch.bursarfeesfines;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.service.BursarExportService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class FeefineactionItemReader implements ItemReader<Feefineaction> {
  private int nextIndex;
  private List<Feefineaction> feefineactions;
  private final BursarExportService exportService;

  public FeefineactionItemReader(BursarExportService exportService) {
    this.exportService = exportService;
    this.nextIndex = 0;
  }

  @Override
  public Feefineaction read() {
    Feefineaction next = null;
    if (nextIndex < feefineactions.size()) {
      next = feefineactions.get(nextIndex);
      nextIndex++;
    } else {
      nextIndex = 0;
    }
    return next;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    JobExecution jobExecution = stepExecution.getJobExecution();
    ExecutionContext jobContext = jobExecution.getExecutionContext();
    List<Account> accounts = (List<Account>) jobContext.get("accounts");

    if (accounts == null || accounts.isEmpty()) {
      feefineactions = Collections.emptyList();
      return;
    }

    List<String> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());
    feefineactions = exportService.findRefundedFeefineActions(accountIds);
  }
}
