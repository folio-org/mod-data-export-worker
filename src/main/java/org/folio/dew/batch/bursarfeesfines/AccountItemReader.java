package org.folio.dew.batch.bursarfeesfines;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.User;
import org.folio.dew.service.BursarExportService;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@StepScope
public class AccountItemReader implements ItemReader<Account> {
  private int nextIndex;
  private List<Account> accounts;
  private Map<String, String> userIdMap;
  private final BursarExportService exportService;
  private StepExecution stepExecution;

  @Value("#{jobParameters['patronGroups']}")
  private String patronGroups;

  @Value("#{jobParameters['daysOutstanding']}")
  private Long daysOutstanding;

  public AccountItemReader(BursarExportService service) {
    this.exportService = service;
    this.accounts = new ArrayList<>();
    this.userIdMap = new HashMap<>();
    this.nextIndex = 0;
  }

  @Override
  public Account read() {
    ExecutionContext stepContext = stepExecution.getExecutionContext();
    stepContext.put("accounts", accounts);
    stepContext.put("userIdMap", userIdMap);

    Account next = null;
    if (nextIndex < accounts.size()) {
      next = accounts.get(nextIndex);
      nextIndex++;
    } else {
      nextIndex = 0;
    }
    return next;
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    this.stepExecution = stepExecution;

    if (daysOutstanding == null || patronGroups == null) {
      log.info("Job parameters don't set");
      return;
    }

    String[] groupIds = patronGroups.split(",");
    List<User> users = exportService.findUsers(Arrays.asList(groupIds));

    userIdMap = users.stream().collect(Collectors.toMap(User::getId, user -> Optional.ofNullable(user.getExternalSystemId()).orElse("")));
    accounts = exportService.findAccounts(daysOutstanding, users);

    this.stepExecution.getExecutionContext().put("userIdMap", userIdMap);
  }
}
