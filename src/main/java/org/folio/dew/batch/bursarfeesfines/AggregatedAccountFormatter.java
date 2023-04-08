package org.folio.dew.batch.bursarfeesfines;

import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.batch.bursarfeesfines.service.BursarTokenFormatter;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class AggregatedAccountFormatter
  implements ItemProcessor<AggregatedAccountsByUser, String> {

  @Value("#{stepExecution}")
  private StepExecution stepExecution;

  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Override
  public String process(@CheckForNull AggregatedAccountsByUser item) {
    if (item == null) {
      return null;
    }

    return jobConfig
      .getData()
      .stream()
      .map(token ->
        BursarTokenFormatter.formatAggregatedAccountsToken(token, item)
      )
      .collect(Collectors.joining());
  }
}
