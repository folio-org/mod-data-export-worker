package org.folio.dew.batch.bursarfeesfines;

import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.utils.ExecutionContextUtils;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
public class FeefineactionItemReader implements ItemReader<Feefineaction> {

  private final BursarExportService exportService;

  private List<Feefineaction> feefineactions;
  private int nextIndex = 0;

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
    List<Account> accounts = (List<Account>) ExecutionContextUtils.getExecutionVariable(stepExecution, "accounts");
    if (accounts == null || accounts.isEmpty()) {
      feefineactions = Collections.emptyList();
      return;
    }

    feefineactions = exportService.findRefundedFeefineActions(accounts.stream().map(Account::getId).collect(Collectors.toList()));
  }

}
