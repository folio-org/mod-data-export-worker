package org.folio.dew.batch.bursarfeesfines.service.impl;

import joptsimple.internal.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.dew.batch.bursarfeesfines.service.BursarExportService;
import org.folio.dew.client.*;
import org.folio.dew.domain.dto.*;
import org.folio.dew.domain.dto.bursarfeesfines.TransferRequest;
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

  private static final String SERVICE_POINT_CODE = "system";
  private static final String DEFAULT_PAYMENT_METHOD = "Bursar";
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
  private final TransferClient transferClient;
  private final ServicePointClient servicePointClient;

  @Override
  public void transferAccounts(List<Account> accounts, BursarFeeFines bursarFeeFines) {
    TransferRequest transferRequest = toTransferRequest(accounts, bursarFeeFines);
    log.info("Creating {}.", transferRequest);
    bulkClient.transferAccount(transferRequest);
  }

  @Override
  public List<User> findUsers(List<String> patronGroups) {
    if (patronGroups == null) {
      log.error("Can not create query for batch job, cause config parameters are null.");
      return Collections.emptyList();
    }

    final String groupIds = patronGroups.stream().collect(toQueryParameters);
    var userResponse = userClient.getUserByQuery(String.format(USER_QUERY, groupIds), DEFAULT_LIMIT);
    if (userResponse == null || userResponse.getTotalRecords() == 0) {
      log.error("There are no active users for patrons group(s) {}.", groupIds);
      return Collections.emptyList();
    }

    return userResponse.getUsers();
  }

  @Override
  public List<Account> findAccounts(Long outStandingDays, List<User> users) {
    if (outStandingDays == null) {
      log.error("Can not create query for batch job, cause outStandingDays are null.");
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

  private TransferRequest toTransferRequest(List<Account> accounts, BursarFeeFines bursarFeeFines) {
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
          String.format("Transfer amount should be positive for account(s) %s", StringUtils.join(accounts, ",")));
    }

    String paymentMethod;
    if (bursarFeeFines.getTransferAccountId() == null) {
      paymentMethod = DEFAULT_PAYMENT_METHOD;
    } else {
      TransferdataCollection transfers = transferClient.get("id==" + bursarFeeFines.getTransferAccountId().toString(), 1);
      paymentMethod = CollectionUtils.isEmpty(transfers.getTransfers()) ?
          DEFAULT_PAYMENT_METHOD :
          transfers.getTransfers().get(0).getAccountName();
    }

    TransferRequest transferRequest = new TransferRequest();
    transferRequest.setAmount(remainingAmount.doubleValue());
    transferRequest.setPaymentMethod(paymentMethod);
    transferRequest.setServicePointId(getServicePoint().getId());
    transferRequest.setNotifyPatron(false);
    transferRequest.setUserName(USER_NAME);
    transferRequest.setAccountIds(accountIds);
    return transferRequest;
  }

  private ServicePoint getServicePoint() {
    ServicePoints servicePoints = servicePointClient.get("code==" + SERVICE_POINT_CODE, 2);
    if (servicePoints.getTotalRecords() < 1) {
      throw new IllegalStateException(
          "Fees/fines bursar report generation needs a service point with '" + SERVICE_POINT_CODE + "' code for transfers creation. Create please such service point and run the export again.");
    }
    if (servicePoints.getTotalRecords() > 1) {
      throw new IllegalStateException(
          "Fees/fines bursar report generation needs a service point with '" + SERVICE_POINT_CODE + "' code for transfers creation. More than one such service points were found - resolve please this ambiguity and run the export again.");
    }
    return servicePoints.getServicepoints().get(0);
  }

  private FeefineactionCollection findFeefineActions(List<String> accountIds) {
    String ids = Strings.join(accountIds, " or ");
    String query = String.format(FEEFINE_QUERY, ids);
    return feefineClient.getFeefineactions(query, DEFAULT_LIMIT);
  }

}
