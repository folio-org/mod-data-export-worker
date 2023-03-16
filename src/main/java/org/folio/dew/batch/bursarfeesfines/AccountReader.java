package org.folio.dew.batch.bursarfeesfines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class AccountReader implements ItemReader<AccountWithAncillaryData> {

  private final BursarExportService exportService;
  private final ObjectMapper objectMapper;

  @Value("#{jobParameters['bursarFeeFines']}")
  private String bursarFeeFinesStr;

  private StepExecution stepExecution;
  private List<Account> accounts = new ArrayList<>();
  private int nextIndex = 0;

  // just to test temporarily
  private boolean createEvenIfEmpty = false;

  @Override
  public AccountWithAncillaryData read() {
    if (nextIndex < accounts.size()) {
      Account next = accounts.get(nextIndex);
      nextIndex++;
      return AccountWithAncillaryData.builder().account(next).build();
    } else {
      nextIndex = 0;
      if (createEvenIfEmpty) {
        createEvenIfEmpty = false;
        return AccountWithAncillaryData.builder().build();
      }
      return null;
    }
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution)
    throws JsonProcessingException {
    this.stepExecution = stepExecution;

    BursarExportJob bursarFeeFines = objectMapper.readValue(
      bursarFeeFinesStr,
      BursarExportJob.class
    );
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("jobConfig", bursarFeeFines);

    log.error("--- Called AccountReader::initStep ---");
    log.error("--- The implementation here is TBD ---");

    // should do some filtering magic here
    accounts = exportService.getAllAccounts();

    stepExecution.getExecutionContext().put("accounts", accounts);

    // initializing a totalAmount variable in jobExecutionContext
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("totalAmount", new BigDecimal(0));
  }
}
