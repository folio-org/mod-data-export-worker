package org.folio.dew.batch.bursarfeesfines;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterEvaluator;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("filteredAccounts", filteredAccounts);
  }
}
