package org.folio.dew.batch.bursarfeesfines;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarTokenFormatter;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
@Log4j2
public class AggregatedAccountFormatter implements ItemProcessor<AggregatedAccountsByUser, String> {

  @Value("#{stepExecution}")
  private StepExecution stepExecution;

  @Value("#{jobParameters['jobId']}")
  private String jobId;

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Value("#{jobExecutionContext['totalAmount']}")
  private BigDecimal currentTotalFeeAmount;

  @Override
  public String process(@CheckForNull AggregatedAccountsByUser item) {
    if (item == null) {
      return null;
    }

    // Update job total amount
    currentTotalFeeAmount = currentTotalFeeAmount.add(item.findTotalAmount());
    stepExecution.getJobExecution()
      .getExecutionContext()
      .put("totalAmount", currentTotalFeeAmount);

    return jobConfig.getData()
      .stream()
      .map(token -> BursarTokenFormatter.formatAggregatedAccountsToken(token, item))
      .collect(Collectors.joining());
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    log.info("In AggregatedAccountFormatter::initStep");
    currentTotalFeeAmount = new BigDecimal(0);
  }
}
