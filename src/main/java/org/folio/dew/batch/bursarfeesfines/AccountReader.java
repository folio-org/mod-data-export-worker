package org.folio.dew.batch.bursarfeesfines;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private Map<String, String> userIdMap = new HashMap<>();
  private int nextIndex = 0;

  @Override
  public AccountWithAncillaryData read() {
    var stepContext = stepExecution.getExecutionContext();
    stepContext.put("accounts", accounts);
    stepContext.put("userIdMap", userIdMap);

    if (nextIndex < accounts.size()) {
      Account next = accounts.get(nextIndex);
      nextIndex++;
      return AccountWithAncillaryData.builder().account(next).build();
    } else {
      nextIndex = 0;
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
      .put("bursarFeeFines", bursarFeeFines);

    log.error("--- Called AccountReader::initStep ---");
    log.error("--- The implementation here is TBD ---");

    // List<User> users = exportService.findUsers();
    // if (CollectionUtils.isEmpty(users)) {
    //   throw new IllegalArgumentException(
    //     String.format(
    //       "Users not found for patron group(s) %s",
    //       StringUtils.join(bursarFeeFines.getPatronGroups(), ",")
    //     )
    //   );
    // }

    // userIdMap =
    //   users
    //     .stream()
    //     .collect(
    //       Collectors.toMap(
    //         User::getId,
    //         user -> Optional.ofNullable(user.getExternalSystemId()).orElse("")
    //       )
    //     );
    // accounts = exportService.findAccounts();

    stepExecution.getExecutionContext().put("userIdMap", userIdMap);
  }
}
