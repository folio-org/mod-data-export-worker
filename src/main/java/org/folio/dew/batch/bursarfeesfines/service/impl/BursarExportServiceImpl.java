package org.folio.dew.batch.bursarfeesfines.service.impl;

import static java.util.stream.Collectors.joining;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.folio.dew.client.FeefineactionsClient;
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
import org.springframework.batch.core.JobParameter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Log4j2
public class BursarExportServiceImpl implements BursarExportService {

  private static final String SERVICE_POINT_CODE = "system";
  private static final String DEFAULT_PAYMENT_METHOD = "Bursar";
  private static final String USER_NAME = "System";
  private static final String ACCOUNT_QUERY =
    "userId==%s and remaining > 0.0 and metadata.createdDate<=%s";
  private static final String USER_QUERY =
    "(active==\"true\" and patronGroup==%s)";
  private static final String FEEFINE_QUERY =
    "(accountId==(%s) and (typeAction==(\"Refunded partially\" or \"Refunded fully\")))";
  private static final long DEFAULT_LIMIT = 10000L;
  private final Collector<CharSequence, ?, String> toQueryParameters = joining(
    " or ",
    "(",
    ")"
  );

  // provided by env
  @Value("${application.bucket.size}")
  private int bucketSize;

  private final ObjectMapper objectMapper;

  // used to query data from Okapi/other modules
  private final UserClient userClient;
  private final InventoryClient inventoryClient;
  private final AccountClient accountClient;
  private final AccountBulkClient bulkClient;
  private final FeefineactionsClient feefineClient;
  private final TransferClient transferClient;
  private final ServicePointClient servicePointClient;

  /** Convert the JobParameter to our schema */
  private BursarExportJob extractBursarFeeFines(JobParameter jobParameter)
    throws com.fasterxml.jackson.core.JsonProcessingException {
    final String value = (String) jobParameter.getValue();
    return objectMapper.readValue(value, BursarExportJob.class);
  }

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

      if (accountsToBeTransferred.size() > 0) {
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
        log.info("Creating {}.", transferRequest);
        bulkClient.transferAccount(transferRequest);
      }
    }

    // transfer non-transferred accounts to account specified in else
    nonTransferredAccountsSet.removeAll(transferredAccountsSet);

    if (nonTransferredAccountsSet.size() > 0) {
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

  // NCO: Some sample queries that were done to get all users and patron groups;
  //        this will likely be combined/expanded to some aggregate thing to find accounts
  //        matching our more complex filter types
  // @Override
  // public List<User> findUsers(List<String> patronGroups) {
  //   if (patronGroups == null) {
  //     log.error("Can not create query for batch job, cause config parameters are null.");
  //     return Collections.emptyList();
  //   }
  //   return fetchDataInBatch(patronGroups, null, (nill, listParams) -> fetchUsers(listParams));
  // }

  // @Override
  // public List<Account> findAccounts(Long outStandingDays, List<User> users) {
  //   if (outStandingDays == null) {
  //     log.error("Can not create query for batch job, cause outStandingDays are null.");
  //     return Collections.emptyList();
  //   }
  // return fetchDataInBatch(users, outStandingDays, (u, d) -> fetchAccounts(d, (Long) u));
  // }

  // @Override
  // public List<Feefineaction> findRefundedFeefineActions(List<String> accountIds) {
  //   if (accountIds.isEmpty()) {
  //     return Collections.emptyList();
  //   }
  //   return fetchDataInBatch(accountIds, null, (nill, paramList) -> fetchFeefineActions(paramList));
  // }

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

  // private List<User> fetchUsers(List<String> patronGroups) {
  //   final String groupIds = patronGroups.stream().collect(toQueryParameters);
  //   var userResponse = userClient.getUserByQuery(String.format(USER_QUERY, groupIds), DEFAULT_LIMIT);
  //   if (userResponse == null || userResponse.getTotalRecords() == 0) {
  //     log.error("There are no active users for patrons group(s) {}.", groupIds);
  //     return Collections.emptyList();
  //   }
  //   return userResponse.getUsers();
  // }

  // private List<Account> fetchAccounts(List<User> users, Long outStandingDays) {
  //   var localDate = LocalDate.now().minusDays(outStandingDays);
  //   final String userIdsAsParameter = users.stream().map(User::getId).collect(toQueryParameters);
  //   var accountQuery = String.format(ACCOUNT_QUERY, userIdsAsParameter, localDate);
  //   return accountClient.getAccounts(accountQuery, DEFAULT_LIMIT).getAccounts();
  // }

  // private List<Feefineaction> fetchFeefineActions(List<String> accountIds) {
  //   var ids = Strings.join(accountIds, " or ");
  //   var query = String.format(FEEFINE_QUERY, ids);
  //   return feefineClient.getFeefineactions(query, DEFAULT_LIMIT).getFeefineactions();
  // }

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
      .get("id==" + transferAccountID, DEFAULT_LIMIT)
      .getTransfers()
      .get(0)
      .getAccountName();
  }
}
