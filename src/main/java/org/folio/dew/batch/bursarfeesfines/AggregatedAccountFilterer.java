package org.folio.dew.batch.bursarfeesfines;

import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterAggregateEvaluator;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterEvaluator;
import org.folio.dew.domain.dto.BursarExportFilterAggregate;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class AggregatedAccountFilterer
  implements ItemProcessor<AggregatedAccountsByUser, AggregatedAccountsByUser> {

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Override
  public AggregatedAccountsByUser process(
    AggregatedAccountsByUser aggregatedAccounts
  ) {
    if (
      BursarFilterAggregateEvaluator.evaluate(
        aggregatedAccounts,
        jobConfig.getGroupByPatronFilter()
      )
    ) {
      return aggregatedAccounts;
    } else {
      return null;
    }
  }
}
