package org.folio.dew.batch.bursarfeesfines;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterEvaluator;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@StepScope
@RequiredArgsConstructor
public class AggregatedAccountReader
  implements ItemReader<AggregatedAccountsByUser> {

  private final BursarExportService exportService;

  private Map<String, User> userMap;
  private Map<String, Item> itemMap;
  private List<Account> accounts = new ArrayList<>();
  private int nextIndex = 0;
  private List<AggregatedAccountsByUser> aggregatedAccountsByUsersList = new ArrayList<>();

  @Value("#{jobExecutionContext['jobConfig']}")
  private BursarExportJob jobConfig;

  @Override
  public AggregatedAccountsByUser read() {
    if (nextIndex < aggregatedAccountsByUsersList.size()) {
      AggregatedAccountsByUser aggregatedAccounts = aggregatedAccountsByUsersList.get(
        nextIndex
      );
      ++nextIndex;
      return aggregatedAccounts;
    } else {
      nextIndex = 0;
      return null;
    }
  }

  @BeforeStep
  public void initStep(StepExecution stepExecution) {
    log.info("--- Called AggregatedAccountReader::initStep ---");

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

    // for loop to create a list of accounts with their respective user and item
    List<AccountWithAncillaryData> accountsWithAncillaryData = new ArrayList<>();
    for (Account account : accounts) {
      AccountWithAncillaryData accountWithAncillaryData = AccountWithAncillaryData
        .builder()
        .account(account)
        .user(userMap.get(account.getUserId()))
        .item(itemMap.getOrDefault(account.getItemId(), null))
        .build();
      if (
        BursarFilterEvaluator.evaluate(
          accountWithAncillaryData,
          jobConfig.getFilter()
        )
      ) {
        accountsWithAncillaryData.add(accountWithAncillaryData);
      }
    }

    // then pass the list through bursarFilterEvaluator
    HashMap<User, List<Account>> userToAccountsListMap = new HashMap<>();
    for (AccountWithAncillaryData accountWithAncillaryData : accountsWithAncillaryData) {
      User user = accountWithAncillaryData.getUser();
      Account account = accountWithAncillaryData.getAccount();

      userToAccountsListMap
        .computeIfAbsent(user, (User key) -> new ArrayList<Account>())
        .add(account);
    }

    // then aggregate them by users. As a result, a list of AggregratedAccountsByUser
    userToAccountsListMap.forEach((User user, List<Account> accountsList) -> {
      aggregatedAccountsByUsersList.add(
        AggregatedAccountsByUser
          .builder()
          .user(user)
          .accounts(accountsList)
          .build()
      );
    });

    log.info(aggregatedAccountsByUsersList.toString());

    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("itemMap", itemMap);

    // initializing a totalAmount variable in jobExecutionContext
    stepExecution
      .getJobExecution()
      .getExecutionContext()
      .put("totalAmount", new BigDecimal(0));
  }
}
