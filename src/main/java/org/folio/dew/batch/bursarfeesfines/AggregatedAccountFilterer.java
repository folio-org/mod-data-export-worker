package org.folio.dew.batch.bursarfeesfines;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterAggregateEvaluator;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
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
public class AggregatedAccountFilterer
  implements ItemProcessor<AggregatedAccountsByUser, AggregatedAccountsByUser> {

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Value("#{jobExecutionContext['itemMap']}")
  private Map<String, Item> itemMap;

  private List<AccountWithAncillaryData> filteredAccounts = new ArrayList<>();

  @Override
  public AggregatedAccountsByUser process(
    AggregatedAccountsByUser aggregatedAccounts
  ) {
    if (
      BursarFilterAggregateEvaluator.evaluateAggregate(
        aggregatedAccounts,
        jobConfig.getGroupByPatronFilter()
      )
    ) {
      // Add all the accounts into aggregated accounts
      for (Account account : aggregatedAccounts.getAccounts()) {
        filteredAccounts.add(
          AccountWithAncillaryData
            .builder()
            .account(account)
            .user(aggregatedAccounts.getUser())
            .item(itemMap.getOrDefault(account.getItemId(), null))
            .build()
        );
      }

      return aggregatedAccounts;
    } else {
      return null;
    }
  }

  @AfterStep
  public void afterStep(StepExecution stepExecution) {
    if (filteredAccounts.isEmpty()) {
      log.error("No accounts matched the aggregate criteria");
      stepExecution.setExitStatus(ExitStatus.FAILED);
      stepExecution.addFailureException(
        new IllegalStateException("No accounts matched the aggregate criteria")
      );
    }
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("filteredAccounts", filteredAccounts);
  }
}
