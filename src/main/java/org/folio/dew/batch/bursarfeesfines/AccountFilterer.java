package org.folio.dew.batch.bursarfeesfines;

import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
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

  private final BursarExportService exportService;

  @Override
  public AccountWithAncillaryData process(AccountWithAncillaryData item) {
    log.error(
      "In AccountFilterer::process (implementation TBD), item={}",
      item
    );

    // if the item should not be included, return null
    return item;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    log.error("In AccountFilterer::initStep (implementation TBD)");
  }
}
