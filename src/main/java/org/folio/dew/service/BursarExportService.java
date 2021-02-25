package org.folio.dew.service;

import java.util.List;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.User;

public interface BursarExportService {

  void transferAccounts(List<Account> accounts);

  List<User> findUsers(List<String> patronGroups);

  List<Account> findAccounts(Long outStandingDays, List<User> users);

  List<Feefineaction> findRefundedFeefineActions(List<String> accountIds);
}
