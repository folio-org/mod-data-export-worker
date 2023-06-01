package org.folio.dew.batch.bursarfeesfines;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import org.folio.dew.batch.bursarfeesfines.service.BursarTokenFormatter;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class AccountFormatter
  implements ItemProcessor<AccountWithAncillaryData, String> {

  @Value("#{stepExecution}")
  private StepExecution stepExecution;

  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Value("#{jobExecutionContext['totalAmount']}")
  private BigDecimal currentTotalFeeAmount;

  @Override
  public String process(@CheckForNull AccountWithAncillaryData item) {
    if (item == null) {
      return null;
    }

    // Update job total amount
    currentTotalFeeAmount =
      currentTotalFeeAmount.add(item.getAccount().getAmount());
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("totalAmount", currentTotalFeeAmount);

    return jobConfig
      .getData()
      .stream()
      .map(token -> BursarTokenFormatter.formatDataToken(token, item))
      .collect(Collectors.joining());
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    currentTotalFeeAmount = new BigDecimal(0);
  }
}
