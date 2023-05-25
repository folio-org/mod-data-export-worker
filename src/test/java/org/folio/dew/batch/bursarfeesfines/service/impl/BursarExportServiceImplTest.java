package org.folio.dew.batch.bursarfeesfines.service.impl;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.TransferClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportFilterNegation;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.BursarExportTransferCriteria;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.Servicepoints;
import org.folio.dew.domain.dto.Transfer;
import org.folio.dew.domain.dto.TransferdataCollection;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.domain.dto.bursarfeesfines.TransferRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({ MockitoExtension.class })
class BursarExportServiceImplTest {

  private static BursarExportService bursarExportService;
  private static UserClient userClient;
  private static InventoryClient inventoryClient;
  private static AccountClient accountClient;
  private static AccountBulkClient bulkClient;
  private static TransferClient transferClient;
  private static ServicePointClient servicePointClient;

  @BeforeAll
  static void setUp() {
    userClient = mock(UserClient.class);
    inventoryClient = mock(InventoryClient.class);
    accountClient = mock(AccountClient.class);
    bulkClient = mock(AccountBulkClient.class);
    transferClient = mock(TransferClient.class);
    servicePointClient = mock(ServicePointClient.class);

    String transferAccountName = "test_name";

    TransferdataCollection transferdataCollection = new TransferdataCollection();
    List<Transfer> transfers = new ArrayList<>();
    Transfer transfer = new Transfer();
    transfer.setAccountName(transferAccountName);
    transfers.add(transfer);
    transferdataCollection.setTransfers(transfers);

    when(transferClient.get("id==00000000-0000-0000-0000-000000000000", 1))
      .thenReturn(transferdataCollection);

    bursarExportService =
      new BursarExportServiceImpl(
        userClient,
        inventoryClient,
        accountClient,
        bulkClient,
        transferClient,
        servicePointClient
      );
  }

  @Test
  void testBursarExportService() {
    List<AccountWithAncillaryData> accounts = new ArrayList<>();
    Account account = new Account();
    account.setId("1111-11-11-11-111111");
    account.setAmount(new BigDecimal(100));
    account.setRemaining(new BigDecimal(100));

    accounts.add(AccountWithAncillaryData.builder().account(account).build());

    BursarExportJob bursarFeeFines = new BursarExportJob();
    BursarExportFilterNegation filterNegation = new BursarExportFilterNegation();
    filterNegation.setCriteria(new BursarExportFilterPass());

    BursarExportTransferCriteria transferCriteria = new BursarExportTransferCriteria();
    List<BursarExportTransferCriteriaConditionsInner> conditions = new ArrayList<>();
    BursarExportTransferCriteriaConditionsInner condition = new BursarExportTransferCriteriaConditionsInner();
    condition.setCondition(new BursarExportFilterPass());
    condition.setAccount(UUID.fromString("0000-00-00-00-000000"));
    conditions.add(condition);
    transferCriteria.setElse(null);

    transferCriteria.setConditions(conditions);
    bursarFeeFines.setFilter(filterNegation);
    bursarFeeFines.setTransferInfo(transferCriteria);

    List<ServicePoint> servicePointsList = new ArrayList<>();
    ServicePoint servicePoint = new ServicePoint();
    servicePoint.setId("00000000-0000-0000-0000-000000000001");
    servicePointsList.add(servicePoint);
    Servicepoints servicepoints = new Servicepoints();
    servicepoints.setServicepoints(servicePointsList);
    servicepoints.setTotalRecords(1);

    when(servicePointClient.get("code==system", 2)).thenReturn(servicepoints);

    bursarExportService.transferAccounts(accounts, bursarFeeFines);

    verify(transferClient, times(1))
      .get("id==00000000-0000-0000-0000-000000000000", 1);
    verify(servicePointClient, times(1)).get("code==system", 2);

    servicepoints.setTotalRecords(0);
    when(servicePointClient.get("code==system", 2)).thenReturn(servicepoints);
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> bursarExportService.transferAccounts(accounts, bursarFeeFines)
    );

    servicepoints.setTotalRecords(2);
    when(servicePointClient.get("code==system", 2)).thenReturn(servicepoints);
    Assertions.assertThrows(
      IllegalStateException.class,
      () -> bursarExportService.transferAccounts(accounts, bursarFeeFines)
    );
  }
}
