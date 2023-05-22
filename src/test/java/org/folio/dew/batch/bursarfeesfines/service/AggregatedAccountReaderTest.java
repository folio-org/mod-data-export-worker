package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.batch.bursarfeesfines.AggregatedAccountReader;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportFilterAmount;
import org.folio.dew.domain.dto.BursarExportFilterAmount.ConditionEnum;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AggregatedAccountReaderTest {

  public static Map<String, User> userMap;
  public static Map<String, Item> itemMap;
  public static List<Account> accounts;
  public static BursarExportJob jobConfig;

  @BeforeAll
  static void setUp() {
    userMap = new HashMap<String, User>();
    for (int i = 0; i < 3; ++i) {
      String userId = String.format("user_%d", i);
      User user = new User();
      user.setId(userId);
      userMap.put(userId, user);
    }

    itemMap = new HashMap<String, Item>();
    for (int i = 0; i < 3; ++i) {
      String itemId = String.format("item_%d", i);
      Item item = new Item();
      item.setId(itemId);
      itemMap.put(itemId, item);
    }

    accounts = new ArrayList<Account>();
    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 2; ++j) {
        Account account = new Account();
        account.setUserId(String.format("user_%d", i));
        account.setItemId(String.format("item_%d", i));
        account.setAmount(new BigDecimal(0));
        accounts.add(account);
      }
    }

    jobConfig = new BursarExportJob();
  }

  @Test
  void testCreateAggregatedAccountsList() {
    List<AggregatedAccountsByUser> expectedList = new ArrayList<>();
    for (int i = 0; i < 3; ++i) {
      List<Account> accounts = new ArrayList<Account>();
      for (int j = 0; j < 2; ++j) {
        Account account = new Account();
        account.setUserId(String.format("user_%d", i));
        account.setItemId(String.format("item_%d", i));
        account.setAmount(new BigDecimal(0));
        accounts.add(account);
      }
      AggregatedAccountsByUser aggregatedAccounts = AggregatedAccountsByUser
        .builder()
        .accounts(accounts)
        .user(userMap.get(String.format("user_%d", i)))
        .build();
      expectedList.add(aggregatedAccounts);
    }

    BursarExportFilterPass filterTrue = new BursarExportFilterPass();
    jobConfig.setFilter(filterTrue);
    List<AggregatedAccountsByUser> resultList = AggregatedAccountReader.createAggregatedAccountsList(
      accounts,
      userMap,
      itemMap,
      jobConfig
    );
    assertThat(resultList.size(), is(3));
    assertThat(resultList, containsInAnyOrder(expectedList.toArray()));

    BursarExportFilterAmount filterFalse = new BursarExportFilterAmount();
    filterFalse.setAmount(10);
    filterFalse.setCondition(ConditionEnum.GREATER_THAN);
    jobConfig.setFilter(filterFalse);
    resultList =
      AggregatedAccountReader.createAggregatedAccountsList(
        accounts,
        userMap,
        itemMap,
        jobConfig
      );
    assertThat(resultList.size(), is(0));
    assertThat(
      resultList,
      containsInAnyOrder(new AggregatedAccountsByUser[] {})
    );
  }
}
