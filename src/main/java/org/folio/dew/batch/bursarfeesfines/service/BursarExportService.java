package org.folio.dew.batch.bursarfeesfines.service;

import java.util.List;
import java.util.Map;
// import org.folio.dew.domain.dto.BursarFeeFinesTypeMapping;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.Feefineaction;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.BursarJobPrameterDto;

public interface BursarExportService {

  // void addMapping(String jobId, Map<String, List<BursarFeeFinesTypeMapping>> mapping);

  // BursarFeeFinesTypeMapping getMapping(String jobId, Account account);

  void transferAccounts(List<Account> accounts, BursarJobPrameterDto bursarFeeFines);

  List<User> findUsers(List<String> patronGroups);

  List<Account> findAccounts(Long outStandingDays, List<User> users);

  List<Feefineaction> findRefundedFeefineActions(List<String> accountIds);

}
