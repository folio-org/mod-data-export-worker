package org.folio.dew.batch.bursarfeesfines;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterEvaluator;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@StepScope
@RequiredArgsConstructor
public class AccountFilterer
  implements ItemProcessor<AccountWithAncillaryData, AccountWithAncillaryData> {

  private List<AccountWithAncillaryData> filteredAccounts = new ArrayList<>();

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Override
  public AccountWithAncillaryData process(AccountWithAncillaryData account) {
    if (BursarFilterEvaluator.evaluate(account, jobConfig.getFilter())) {
      filteredAccounts.add(account);
      return account;
    } else {
      return null;
    }
  }

  @AfterStep
  public void afterStep(StepExecution stepExecution) {
    if (filteredAccounts.isEmpty()) {
      log.error("No accounts matched the criteria");
      stepExecution.setExitStatus(ExitStatus.FAILED);
      stepExecution.addFailureException(
        new IllegalStateException("No accounts matched the criteria")
      );
    }
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("filteredAccounts", filteredAccounts);
  }
}
