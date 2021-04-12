package org.folio.dew.batch.bursarfeesfines.service;

import org.folio.des.domain.dto.BursarFeeFines;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.User;

import java.util.List;

public interface BursarExportService {

  void transferAccounts(List<Account> accounts, BursarFeeFines bursarFeeFines);

  List<User> findUsers(List<String> patronGroups);

  List<Account> findAccounts(Long outStandingDays, List<User> users);

  List<Feefineaction> findRefundedFeefineActions(List<String> accountIds);

}
