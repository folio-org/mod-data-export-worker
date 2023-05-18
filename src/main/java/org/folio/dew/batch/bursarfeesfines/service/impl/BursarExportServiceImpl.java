package org.folio.dew.batch.bursarfeesfines.service.impl;

import static java.util.stream.Collectors.joining;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.batch.bursarfeesfines.service.BursarFilterEvaluator;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.TransferClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.AccountdataCollection;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.BursarExportTransferCriteriaConditionsInner;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.folio.dew.domain.dto.bursarfeesfines.TransferRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Log4j2
public class BursarExportServiceImpl implements BursarExportService {

  private static final String SERVICE_POINT_CODE = "system";
  private static final String USER_NAME = "System";
  private static final long DEFAULT_LIMIT = 10000L;
  private final Collector<CharSequence, ?, String> toQueryParameters = joining(
    " or ",
    "(",
    ")"
  );

  // provided by env
  @Value("${application.bucket.size}")
  private int bucketSize;

  // used to query data from Okapi/other modules
  private final UserClient userClient;
  private final InventoryClient inventoryClient;
  private final AccountClient accountClient;
  private final AccountBulkClient bulkClient;
  private final TransferClient transferClient;
  private final ServicePointClient servicePointClient;

  /** Take the found fine accounts and mark them as transferred */
  @Override
  public void transferAccounts(
    List<AccountWithAncillaryData> accounts,
    BursarExportJob bursarFeeFines
  ) {
    Set<AccountWithAncillaryData> transferredAccountsSet = new HashSet<>();
    Set<AccountWithAncillaryData> nonTransferredAccountsSet = new HashSet<>(
      accounts
    );

    for (BursarExportTransferCriteriaConditionsInner bursarExportTransferCriteriaConditionsInner : bursarFeeFines
      .getTransferInfo()
      .getConditions()) {
      List<AccountWithAncillaryData> accountsToBeTransferred = accounts
        .stream()
        .filter(account ->
          BursarFilterEvaluator.evaluate(
            account,
            bursarExportTransferCriteriaConditionsInner.getCondition()
          )
        )
        .collect(Collectors.toList());

      if (!accountsToBeTransferred.isEmpty()) {
        transferredAccountsSet.addAll(accountsToBeTransferred);

        String accountName = getTransferAccountName(
          bursarExportTransferCriteriaConditionsInner.getAccount().toString()
        );

        log.info(
          "transferring accounts for filter condition: " +
          bursarExportTransferCriteriaConditionsInner.getCondition().toString()
        );
        TransferRequest transferRequest = toTransferRequest(
          accountsToBeTransferred,
          accountName
        );
        log.info("Transferring {}.", transferRequest);
        bulkClient.transferAccount(transferRequest);
      }
    }

    // transfer non-transferred accounts to account specified in else
    nonTransferredAccountsSet.removeAll(transferredAccountsSet);

    if (!nonTransferredAccountsSet.isEmpty()) {
      String accountName = getTransferAccountName(
        bursarFeeFines.getTransferInfo().getElse().getAccount().toString()
      );

      TransferRequest transferRequest = toTransferRequest(
        nonTransferredAccountsSet.stream().toList(),
        accountName
      );
      log.info("Creating {}.", transferRequest);
      bulkClient.transferAccount(transferRequest);
    }
  }

  @Override
  public List<Account> getAllAccounts() {
    List<Account> accounts = new ArrayList<>();

    AccountdataCollection response = accountClient.getAccounts(
      "remaining > 0.0",
      DEFAULT_LIMIT
    );
    int total = response.getTotalRecords();
    accounts.addAll(response.getAccounts());
    while (accounts.size() < total) {
      response =
        accountClient.getAccounts(
          "remaining > 0.0",
          DEFAULT_LIMIT,
          accounts.size()
        );
      accounts.addAll(response.getAccounts());
    }

    return accounts;
  }

  @Override
  public Map<String, User> getUsers(Set<String> userIds) {
    Map<String, User> map = new HashMap<>();

    if (userIds.isEmpty()) {
      return map;
    }

    List<User> users = fetchDataInBatch(
      new ArrayList<String>(userIds),
      partition ->
        userClient
          .getUserByQuery(
            String.format(
              "id==(%s)",
              partition.stream().collect(toQueryParameters)
            ),
            bucketSize
          )
          .getUsers()
    );

    users.forEach(user -> map.put(user.getId(), user));

    return map;
  }

  @Override
  public Map<String, Item> getItems(Set<String> itemIds) {
    Map<String, Item> map = new HashMap<>();

    if (itemIds.isEmpty()) {
      return map;
    }

    List<Item> items = fetchDataInBatch(
      new ArrayList<String>(itemIds),
      partition ->
        inventoryClient
          .getItemByQuery(
            String.format(
              "id==(%s)",
              partition.stream().collect(toQueryParameters)
            ),
            bucketSize
          )
          .getItems()
    );

    items.forEach(item -> map.put(item.getId(), item));

    return map;
  }

  private <T, P> List<T> fetchDataInBatch(
    List<P> parameters,
    Function<List<P>, List<T>> client
  ) {
    if (parameters.size() <= bucketSize) {
      log.debug("Fetch data by one call");
      return client.apply(parameters);
    }

    final List<List<P>> partition = ListUtils.partition(parameters, bucketSize);
    log.debug("Fetch data in several calls, bucket count {}", partition::size);
    return partition
      .stream()
      .map(paramBucket -> client.apply(paramBucket))
      .collect(ArrayList::new, List::addAll, List::addAll);
  }

  private TransferRequest toTransferRequest(
    List<AccountWithAncillaryData> accounts,
    String accountName
  ) {
    if (CollectionUtils.isEmpty(accounts)) {
      throw new IllegalArgumentException(
        "No accounts found to make transfer request for"
      );
    }

    BigDecimal remainingAmount = BigDecimal.ZERO;
    List<String> accountIds = new ArrayList<>();
    for (AccountWithAncillaryData accountWithAncillaryData : accounts) {
      remainingAmount =
        remainingAmount.add(
          accountWithAncillaryData.getAccount().getRemaining()
        );
      accountIds.add(accountWithAncillaryData.getAccount().getId());
    }

    if (remainingAmount.doubleValue() <= 0) {
      throw new IllegalArgumentException(
        String.format(
          "Transfer amount should be positive for account(s) %s",
          StringUtils.join(accounts, ",")
        )
      );
    }

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAmount(remainingAmount.doubleValue());
    transferRequest.setPaymentMethod(accountName);
    transferRequest.setServicePointId(getSystemServicePoint().getId());
    transferRequest.setNotifyPatron(false);
    transferRequest.setUserName(USER_NAME);
    transferRequest.setAccountIds(accountIds);
    return transferRequest;
  }

  /**
   * Gets the `system` service point, failing if it cannot be found.
   *
   * This is the service point which is used to mark fines as transferred by this process.
   */
  private ServicePoint getSystemServicePoint() {
    var servicePoints = servicePointClient.get(
      "code==" + SERVICE_POINT_CODE,
      2
    );
    if (servicePoints.getTotalRecords() < 1) {
      throw new IllegalStateException(
        "Fees/fines bursar report generation requires a service point with the code '" +
        SERVICE_POINT_CODE +
        "'. Please create this service point and run the export again."
      );
    }
    if (servicePoints.getTotalRecords() > 1) {
      throw new IllegalStateException(
        "Fees/fines bursar report generation requires a service point with the code '" +
        SERVICE_POINT_CODE +
        "'. More than one such service points were found - please resolve this ambiguity and run the export again."
      );
    }
    return servicePoints.getServicepoints().get(0);
  }

  /**
   * Get the transfer account name given a transfer account id
   * @param transferAccountID transfer account ID
   * @return transfer account name
   */
  private String getTransferAccountName(String transferAccountID) {
    return transferClient
      .get("id==" + transferAccountID, 1)
      .getTransfers()
      .get(0)
      .getAccountName();
  }
}
