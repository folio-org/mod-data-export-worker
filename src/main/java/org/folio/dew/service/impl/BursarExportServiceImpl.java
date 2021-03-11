package org.folio.dew.service.impl;

import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.FeefineactionsClient;
import org.folio.dew.client.UserClient;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.FeefineactionCollection;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.TransferRequest;
import org.folio.dew.service.BursarExportService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;

import static java.util.stream.Collectors.joining;

@RequiredArgsConstructor
@Service
@Log4j2
public class BursarExportServiceImpl implements BursarExportService {

  private static final String SERVICE_POINT_ONLINE = "7c5abc9f-f3d7-4856-b8d7-6712462ca007";
  private static final String PAYMENT_METHOD = "Bursar";
  private static final String USER_NAME = "System";
  private static final String ACCOUNT_QUERY = "userId==%s and remaining > 0.0 and metadata.createdDate>=%s";
  private static final String USER_QUERY = "(active==\"true\" and patronGroup==%s)";
  private static final String FEEFINE_QUERY = "(accountId==(%s) and (typeAction==(\"Refunded partially\" or \"Refunded fully\")))";
  private static final long DEFAULT_LIMIT = 10000L;
  private final Collector<CharSequence, ?, String> toQueryParameters = joining(" or ", "(", ")");

  private final UserClient userClient;
  private final AccountClient accountClient;
  private final AccountBulkClient bulkClient;
  private final FeefineactionsClient feefineClient;

  @Override
  public void transferAccounts(List<Account> accounts) {
    TransferRequest request = toTransferRequest(accounts);
    bulkClient.transferAccount(request);
  }

  @Override
  public List<User> findUsers(List<String> patronGroups) {
    if (patronGroups == null) {
      log.info("Can not create query for batch job, cause config parameters are null");
      return Collections.emptyList();
    }

    final String groupIds = patronGroups.stream().collect(toQueryParameters);
    var userResponse = userClient.getUserByQuery(String.format(USER_QUERY, groupIds), DEFAULT_LIMIT);
    if (userResponse == null || userResponse.getTotalRecords() == 0) {
      log.info("There are no active users for patrons groups {}", groupIds);
      return Collections.emptyList();
    }

    return userResponse.getUsers();
  }

  @Override
  public List<Account> findAccounts(Long outStandingDays, List<User> users) {

    if (outStandingDays == null) {
      log.info("Can not create query for batch job, cause outStandingDays are null");
      return Collections.emptyList();
    }
    final LocalDate localDate = LocalDate.now().minusDays(outStandingDays);
    final String userIdsAsParameter = users.stream().map(User::getId).collect(toQueryParameters);
    final String accountQuery = String.format(ACCOUNT_QUERY, userIdsAsParameter, localDate);
    return accountClient.getAccounts(accountQuery, DEFAULT_LIMIT).getAccounts();
  }

  @Override
  public List<Feefineaction> findRefundedFeefineActions(List<String> accountIds) {
    if (accountIds.isEmpty()) {
      return Collections.emptyList();
    }
    return findFeefineActions(accountIds).getFeefineactions();
  }

  private TransferRequest toTransferRequest(List<Account> accounts) {
    BigDecimal remainingAmount = BigDecimal.ZERO;
    List<String> accountIds = new ArrayList<>();

    for (Account account : accounts) {
      remainingAmount = remainingAmount.add(account.getRemaining());
      accountIds.add(account.getId());
    }

    if (remainingAmount.doubleValue() <= 0) {
      throw new IllegalArgumentException(
          String.format("Transfer amount should be positive, accounts [%s]", StringUtils.join(accounts, ",")));
    }

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAmount(remainingAmount.doubleValue());
    transferRequest.setPaymentMethod(PAYMENT_METHOD);
    transferRequest.setServicePointId(SERVICE_POINT_ONLINE);
    transferRequest.setNotifyPatron(false);
    transferRequest.setUserName(USER_NAME);
    transferRequest.setAccountIds(accountIds);
    return transferRequest;
  }

  private FeefineactionCollection findFeefineActions(List<String> accountIds) {
    String ids = Strings.join(accountIds, " or ");
    String query = String.format(FEEFINE_QUERY, ids);
    return feefineClient.getFeefineactions(query, DEFAULT_LIMIT);
  }

}
