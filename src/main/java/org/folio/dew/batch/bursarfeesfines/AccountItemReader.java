package org.folio.dew.batch.bursarfeesfines;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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

import java.util.*;
import java.util.stream.Collectors;

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
    exportService = service;
    accounts = new ArrayList<>();
    userIdMap = new HashMap<>();
    nextIndex = 0;
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

    if (daysOutstanding == null || StringUtils.isBlank(patronGroups)) {
      throw new IllegalArgumentException("'daysOutstanding' and/or 'patronGroups' aren't set");
    }

    List<User> users = exportService.findUsers(Arrays.asList(patronGroups.split(",")));
    if (CollectionUtils.isEmpty(users)) {
      throw new IllegalArgumentException(String.format("Users not found for patron group(s) %s", patronGroups));
    }

    userIdMap = users.stream()
        .collect(Collectors.toMap(User::getId, user -> Optional.ofNullable(user.getExternalSystemId()).orElse("")));
    accounts = exportService.findAccounts(daysOutstanding, users);

    stepExecution.getExecutionContext().put("userIdMap", userIdMap);
  }

}
