package org.folio.dew.batch.bursarfeesfines.service.impl;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.TransferClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.config.JacksonConfiguration;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportFilterNegation;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.BursarExportTransferCriteria;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaElse;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.Servicepoints;
import org.folio.dew.domain.dto.Transfer;
import org.folio.dew.domain.dto.TransferdataCollection;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.UserCollection;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

@ExtendWith({ MockitoExtension.class })
@SpringBootTest(
  classes = { JacksonConfiguration.class, BursarExportServiceImpl.class }
)
@MockBeans(
  {
    @MockBean(AccountClient.class),
    @MockBean(InventoryClient.class),
    @MockBean(UserClient.class),
    @MockBean(AccountBulkClient.class),
    @MockBean(TransferClient.class),
    @MockBean(ServicePointClient.class),
  }
)
@Log4j2
class BursarExportServiceImplTest {

  @Autowired
  private BursarExportService service;

  @MockBean
  private UserClient userClient;

  @MockBean
  private TransferClient transferClient;

  @MockBean
  private ServicePointClient servicePointClient;

  @MockBean
  private AccountClient accountClient;

  @MockBean
  private AccountBulkClient bulkClient;

  @MockBean
  private InventoryClient inventoryClient;

  @Test
  void testBursarExportService() {
    List<AccountWithAncillaryData> accounts = new ArrayList<>();
    Account account = new Account();
    account.setId("1111-11-11-11-111111");
    account.setAmount(new BigDecimal(100));
    account.setRemaining(new BigDecimal(100));

    accounts.add(AccountWithAncillaryData.builder().account(account).build());

    BursarExportJob bursarFeeFines = new BursarExportJob();

    BursarExportTransferCriteria transferCriteria = new BursarExportTransferCriteria();
    List<BursarExportTransferCriteriaConditionsInner> conditions = new ArrayList<>();
    BursarExportTransferCriteriaConditionsInner condition = new BursarExportTransferCriteriaConditionsInner();
    condition.setCondition(new BursarExportFilterPass());
    condition.setAccount(UUID.fromString("0000-00-00-00-000000"));
    conditions.add(condition);
    transferCriteria.setElse(null);

    transferCriteria.setConditions(conditions);
    bursarFeeFines.setFilter(new BursarExportFilterPass());
    bursarFeeFines.setTransferInfo(transferCriteria);

    List<ServicePoint> servicePointsList = mockServicePointsList();
    Servicepoints servicepoints = new Servicepoints();
    servicepoints.setServicepoints(servicePointsList);
    servicepoints.setTotalRecords(1);

    TransferdataCollection transferdataCollection = mockTransferDataCollection();

    when(servicePointClient.get("code==system", 2)).thenReturn(servicepoints);
    when(transferClient.get("id==00000000-0000-0000-0000-000000000000", 1))
      .thenReturn(transferdataCollection);

    service.transferAccounts(accounts, bursarFeeFines);

    BursarExportFilterNegation filterNegation = new BursarExportFilterNegation();
    filterNegation.setCriteria(new BursarExportFilterPass());
    condition.setCondition(filterNegation);
    condition.setAccount(UUID.fromString("0000-00-00-00-000000"));
    conditions.clear();
    conditions.add(condition);
    BursarExportTransferCriteriaElse transferCriteriaElse = new BursarExportTransferCriteriaElse();
    transferCriteriaElse.setAccount(UUID.fromString("0000-00-00-00-000000"));
    transferCriteria.setElse(transferCriteriaElse);

    service.transferAccounts(accounts, bursarFeeFines);

    verify(transferClient, times(2))
      .get("id==00000000-0000-0000-0000-000000000000", 1);
    verify(servicePointClient, times(2)).get("code==system", 2);

    // test exceptions in getSystemServicePoint()
    servicepoints.setTotalRecords(0);
    when(servicePointClient.get("code==system", 2)).thenReturn(servicepoints);
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> service.transferAccounts(accounts, bursarFeeFines)
    );

    servicepoints.setTotalRecords(2);
    when(servicePointClient.get("code==system", 2)).thenReturn(servicepoints);
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> service.transferAccounts(accounts, bursarFeeFines)
    );

    // test exceptions in toTransferRequest()
    account.setRemaining(new BigDecimal(0));
    accounts.add(AccountWithAncillaryData.builder().account(account).build());
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> service.transferAccounts(accounts, bursarFeeFines)
    );
  }

  @Test
  void testFetchDataInBatch() {
    Set<String> userIds = generateUserIds(100);
    UserCollection userCollection = new UserCollection();
    List<User> users = new ArrayList<>();
    users.add(new User());
    userCollection.setUsers(new ArrayList<>());

    when(userClient.getUserByQuery(any(), eq(50L)))
      .thenAnswer(
        new Answer<UserCollection>() {
          @Override
          public UserCollection answer(InvocationOnMock invocation)
            throws Throwable {
            // Generate a random value using mockUserCollection()
            UserCollection randomUserCollection = mockUserCollection();
            return randomUserCollection;
          }
        }
      );
    Assertions.assertEquals(2, service.getUsers(userIds).size());
  }

  private TransferdataCollection mockTransferDataCollection() {
    String transferAccountName = "test_name";

    TransferdataCollection transferdataCollection = new TransferdataCollection();
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.setAccountName(transferAccountName);
    transfers.add(transfer);
    transferdataCollection.setTransfers(transfers);

    return transferdataCollection;
  }

  private List<ServicePoint> mockServicePointsList() {
    List<ServicePoint> servicePointsList = new ArrayList<>();
    ServicePoint servicePoint = new ServicePoint();
    servicePoint.setId("00000000-0000-0000-0000-000000000001");
    servicePointsList.add(servicePoint);

    return servicePointsList;
  }

  private Set<String> generateUserIds(int size) {
    return Stream
      .generate(UUID::randomUUID)
      .limit(size)
      .map(UUID::toString)
      .collect(Collectors.toSet());
  }

  private UserCollection mockUserCollection() {
    UserCollection userCollection = new UserCollection();
    User user = new User().id(UUID.randomUUID().toString());
    userCollection.setUsers(List.of(user));

    log.debug("In mockUserCollection(): {}", userCollection.toString());
    return userCollection;
  }
}
