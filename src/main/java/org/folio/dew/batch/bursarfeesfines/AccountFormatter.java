package org.folio.dew.batch.bursarfeesfines;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.batch.bursarfeesfines.service.BursarTokenFormatter;
import org.folio.dew.domain.dto.BursarExportJob;
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
public class AccountFormatter
  implements ItemProcessor<AccountWithAncillaryData, String> {

  private final BursarExportService exportService;

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
    log.error("In AccountFormatter::initStep (implementation TBD, if any)");
    currentTotalFeeAmount = new BigDecimal(0);
  }
}