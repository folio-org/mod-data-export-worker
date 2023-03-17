package org.folio.dew.batch.bursarfeesfines;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterEvaluator;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
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

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Override
  public AccountWithAncillaryData process(AccountWithAncillaryData account) {
    if (BursarFilterEvaluator.evaluate(account, jobConfig.getFilter())) {
      return account;
    } else {
      return null;
    }
  }
}
