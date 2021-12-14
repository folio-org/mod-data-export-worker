package org.folio.dew.batch.bursarfeesfines.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.FeefineactionsClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.TransferClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.config.JacksonConfiguration;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.AccountdataCollection;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.FeefineactionCollection;
import org.folio.dew.domain.dto.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

@SpringBootTest(classes = {JacksonConfiguration.class, BursarExportServiceImpl.class})
@MockBeans({
  @MockBean(UserClient.class),
  @MockBean(AccountBulkClient.class),
  @MockBean(TransferClient.class),
  @MockBean(ServicePointClient.class)
})
class BursarExportServiceImplTest {
  @Autowired
  private BursarExportServiceImpl bursarExportService;
  @MockBean
  private AccountClient client;
  @MockBean
  private FeefineactionsClient feefineactionsClient;

  @Test
  @DisplayName("Find accounts should return empty list for not defined outStanding days")
  void findAccountsEmptyTest() {
    final List<User> users = generateUsers(100);
    final List<Account> accounts = bursarExportService.findAccounts(null, users);

    assertTrue(accounts.isEmpty());
  }

  @Test
  @DisplayName("Find accounts should fetch data in several buckets")
  void findAccountsTest() {
    when(client.getAccounts(any(), eq(10000L))).thenReturn(mockAccountData());

    final List<User> users = generateUsers(100);
    final List<Account> accounts = bursarExportService.findAccounts(1L, users);

    assertEquals(2, accounts.size());
  }

  @Test
  @DisplayName("Find accounts should fetch data in one call")
  void findAccountsLessThanBucketSizeTest() {
    when(client.getAccounts(any(), eq(10000L))).thenReturn(mockAccountData());

    final List<User> users = generateUsers(50);
    final List<Account> accounts = bursarExportService.findAccounts(1L, users);

    assertEquals(1, accounts.size());
  }

  @Test
  @DisplayName("Find FeeFineActions should return empty collection for no accountIds")
  void findFeeFineActionsEmptyTest() {
    final List<String> accountIds = Collections.emptyList();
    final List<Feefineaction> feefineActions = bursarExportService.findRefundedFeefineActions(accountIds);

    assertTrue(feefineActions.isEmpty());
  }

  @Test
  @DisplayName("Find FeeFineActions should fetch data in several buckets")
  void findFeeFineActionsTest() {
    when(feefineactionsClient.getFeefineactions(any(), eq(10000L))).thenReturn(mockFeeFineData());

    final List<String> accountIds = generateAccountIds(100);
    final List<Feefineaction> feefineActions = bursarExportService.findRefundedFeefineActions(accountIds);

    assertEquals(2, feefineActions.size());
  }

  @Test
  @DisplayName("Find FeeFineActions should fetch data in one call")
  void findFeeFineActionsLessThanBucketSizeTest() {
    when(feefineactionsClient.getFeefineactions(any(), eq(10000L))).thenReturn(mockFeeFineData());

    final List<String> accountIds = generateAccountIds(50);
    final List<Feefineaction> feefineActions = bursarExportService.findRefundedFeefineActions(accountIds);

    assertEquals(1, feefineActions.size());
  }

  private List<User> generateUsers(int size) {
    return Stream
      .generate(UUID::randomUUID)
      .map(UUID::toString)
      .map(id -> new User().id(id))
      .limit(size)
      .collect(Collectors.toList());
  }

  private List<String> generateAccountIds(int size) {
    return Stream
      .generate(UUID::randomUUID)
      .map(UUID::toString)
      .limit(size)
      .collect(Collectors.toList());
  }

  private AccountdataCollection mockAccountData() {
    AccountdataCollection accounts = new AccountdataCollection();
    Account account = new Account().id(UUID.randomUUID().toString());
    accounts.accounts(List.of(account));
    return accounts;
  }

  private FeefineactionCollection mockFeeFineData() {
    FeefineactionCollection collection = new FeefineactionCollection();
    Feefineaction feefineaction = new Feefineaction().accountId(UUID.randomUUID().toString());
    collection.feefineactions(List.of(feefineaction));
    return collection;
  }
}
