package org.folio.dew.batch.bursarfeesfines.service.impl;

import static java.util.stream.Collectors.joining;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collector;
import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.FeefineactionsClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.TransferClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.ServicePoint;
import org.folio.dew.domain.dto.TransferdataCollection;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.BursarJobPrameterDto;
import org.folio.dew.domain.dto.bursarfeesfines.TransferRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Log4j2
public class BursarExportServiceImpl implements BursarExportService {

  private static final String SERVICE_POINT_CODE = "system";
  private static final String DEFAULT_PAYMENT_METHOD = "Bursar";
  private static final String USER_NAME = "System";
  private static final String ACCOUNT_QUERY = "userId==%s and remaining > 0.0 and metadata.createdDate<=%s";
  private static final String USER_QUERY = "(active==\"true\" and patronGroup==%s)";
  private static final String FEEFINE_QUERY = "(accountId==(%s) and (typeAction==(\"Refunded partially\" or \"Refunded fully\")))";
  private static final long DEFAULT_LIMIT = 10000L;
  private final Collector<CharSequence, ?, String> toQueryParameters = joining(" or ", "(", ")");

  @Value("${application.bucket.size}")
  private int bucketSize;

  private final UserClient userClient;
  private final AccountClient accountClient;
  private final AccountBulkClient bulkClient;
  private final FeefineactionsClient feefineClient;
  private final TransferClient transferClient;
  private final ServicePointClient servicePointClient;
  // private final Map<String, Map<String, List<BursarFeeFinesTypeMapping>>> mapping = new ConcurrentHashMap<>();


  // @Override
  // public void addMapping(String jobId, Map<String, List<BursarFeeFinesTypeMapping>> mapping) {
  //   this.mapping.put(jobId, mapping);
  // }

  // @Override
  // public BursarFeeFinesTypeMapping getMapping(String jobId, Account account) {

  //   if (!mapping.containsKey(jobId)) {
  //     return null;
  //   }

  //   final List<BursarFeeFinesTypeMapping> feeFinesTypeMappingList = mapping
  //     .get(jobId)
  //     .get(account.getOwnerId());

  //   if (feeFinesTypeMappingList == null) {
  //     return null;
  //   }

  //   return feeFinesTypeMappingList
  //     .stream()
  //     .filter(m -> m.getFeefineTypeId().toString().equals(account.getFeeFineId()))
  //     .findFirst()
  //     .orElse(null);
  // }

  @Override
  public void transferAccounts(List<Account> accounts, BursarJobPrameterDto bursarFeeFines) {
    var transferRequest = toTransferRequest(accounts, bursarFeeFines);
    log.info("Creating {}.", transferRequest);
    bulkClient.transferAccount(transferRequest);
  }

  @Override
  public List<User> findUsers(List<String> patronGroups) {
    if (patronGroups == null) {
      log.error("Can not create query for batch job, cause config parameters are null.");
      return Collections.emptyList();
    }
    return fetchDataInBatch(patronGroups, null, (nill, listParams) -> fetchUsers(listParams));
  }

  @Override
  public List<Account> findAccounts(Long outStandingDays, List<User> users) {
    if (outStandingDays == null) {
      log.error("Can not create query for batch job, cause outStandingDays are null.");
      return Collections.emptyList();
    }
    return fetchDataInBatch(users, outStandingDays, (u, d) -> fetchAccounts(d, (Long) u));
  }

  @Override
  public List<Feefineaction> findRefundedFeefineActions(List<String> accountIds) {
    if (accountIds.isEmpty()) {
      return Collections.emptyList();
    }
    return fetchDataInBatch(accountIds, null, (nill, paramList) -> fetchFeefineActions(paramList));
  }

  private <T, P> List<T> fetchDataInBatch(
      List<P> parameters, Object additionalParam, BiFunction<Object, List<P>, List<T>> client) {

    if (parameters.size() <= bucketSize) {
      log.debug("Fetch data by one call");
      return client.apply(additionalParam, parameters);
    }

    final List<List<P>> partition = ListUtils.partition(parameters, bucketSize);
    log.debug("Fetch data in several calls, bucket count {}", partition::size);
    return partition.stream()
        .map(paramBucket -> client.apply(additionalParam, paramBucket))
        .collect(ArrayList::new, List::addAll, List::addAll);
  }

  private List<User> fetchUsers(List<String> patronGroups) {
    final String groupIds = patronGroups.stream().collect(toQueryParameters);
    var userResponse = userClient.getUserByQuery(String.format(USER_QUERY, groupIds), DEFAULT_LIMIT);
    if (userResponse == null || userResponse.getTotalRecords() == 0) {
      log.error("There are no active users for patrons group(s) {}.", groupIds);
      return Collections.emptyList();
    }
    return userResponse.getUsers();
  }

  private List<Account> fetchAccounts(List<User> users, Long outStandingDays) {
    var localDate = LocalDate.now().minusDays(outStandingDays);
    final String userIdsAsParameter = users.stream().map(User::getId).collect(toQueryParameters);
    var accountQuery = String.format(ACCOUNT_QUERY, userIdsAsParameter, localDate);
    return accountClient.getAccounts(accountQuery, DEFAULT_LIMIT).getAccounts();
  }

  private List<Feefineaction> fetchFeefineActions(List<String> accountIds) {
    var ids = Strings.join(accountIds, " or ");
    var query = String.format(FEEFINE_QUERY, ids);
    return feefineClient.getFeefineactions(query, DEFAULT_LIMIT).getFeefineactions();
  }

  private TransferRequest toTransferRequest(List<Account> accounts, BursarJobPrameterDto bursarFeeFines) {
    if (CollectionUtils.isEmpty(accounts)) {
      throw new IllegalArgumentException("No accounts found to make transfer request for");
    }

    BigDecimal remainingAmount = BigDecimal.ZERO;
    List<String> accountIds = new ArrayList<>();
    for (Account account : accounts) {
      remainingAmount = remainingAmount.add(account.getRemaining());
      accountIds.add(account.getId());
    }

    if (remainingAmount.doubleValue() <= 0) {
      throw new IllegalArgumentException(
          String.format(
              "Transfer amount should be positive for account(s) %s",
              StringUtils.join(accounts, ",")));
    }

    String paymentMethod;
    if (bursarFeeFines.getTransferAccountId() == null) {
      paymentMethod = DEFAULT_PAYMENT_METHOD;
    } else {
      TransferdataCollection transfers =
          transferClient.get("id==" + bursarFeeFines.getTransferAccountId().toString(), 1);
      paymentMethod =
          CollectionUtils.isEmpty(transfers.getTransfers())
              ? DEFAULT_PAYMENT_METHOD
              : transfers.getTransfers().get(0).getAccountName();
    }

    var transferRequest = new TransferRequest();
    transferRequest.setAmount(remainingAmount.doubleValue());
    transferRequest.setPaymentMethod(paymentMethod);
    transferRequest.setServicePointId(getServicePoint().getId());
    transferRequest.setNotifyPatron(false);
    transferRequest.setUserName(USER_NAME);
    transferRequest.setAccountIds(accountIds);
    return transferRequest;
  }

  private ServicePoint getServicePoint() {
    var servicePoints = servicePointClient.get("code==" + SERVICE_POINT_CODE, 2);
    if (servicePoints.getTotalRecords() < 1) {
      throw new IllegalStateException(
          "Fees/fines bursar report generation requires a service point with the code '"
              + SERVICE_POINT_CODE
              + "'. Please create this service point and run the export again.");
    }
    if (servicePoints.getTotalRecords() > 1) {
      throw new IllegalStateException(
          "Fees/fines bursar report generation requires a service point with the code '"
              + SERVICE_POINT_CODE
              + "'. More than one such service points were found - please resolve this ambiguity and run the export again.");
    }
    return servicePoints.getServicepoints().get(0);
  }
}
