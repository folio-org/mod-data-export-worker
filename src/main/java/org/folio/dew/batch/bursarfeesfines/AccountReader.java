package org.folio.dew.batch.bursarfeesfines;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
@Log4j2
@RequiredArgsConstructor
public class AccountReader implements ItemReader<AccountWithAncillaryData> {

  private final BursarExportService exportService;

  private Map<String, User> userMap;
  private Map<String, Item> itemMap;
  private List<Account> accounts = new ArrayList<>();
  private int nextIndex = 0;

  @Override
  public AccountWithAncillaryData read() {
    if (nextIndex < accounts.size()) {
      Account account = accounts.get(nextIndex);
      nextIndex++;
      return AccountWithAncillaryData
        .builder()
        .account(account)
        .user(userMap.get(account.getUserId()))
        .item(itemMap.getOrDefault(account.getItemId(), null))
        .build();
    } else {
      nextIndex = 0;
      return null;
    }
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    log.error("--- Called AccountReader::initStep ---");

    // TODO: should do some proactive filtering magic here
    // grabbing accounts before users/items because, with a relatively
    // frequent transfer process, there will be less accounts than users
    accounts = exportService.getAllAccounts();
    stepExecution.getExecutionContext().put("accounts", accounts);

    Set<String> userIds = new HashSet<>(
      accounts.stream().map(Account::getUserId).toList()
    );
    Set<String> itemIds = new HashSet<>(
      accounts.stream().map(Account::getItemId).toList()
    );

    userMap = exportService.getUsers(userIds);
    itemMap = exportService.getItems(itemIds);

    // initializing a totalAmount variable in jobExecutionContext
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("totalAmount", new BigDecimal(0));
  }
}
