package org.folio.dew.batch.bursarfeesfines.service;

import java.util.List;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportJob;

public interface BursarExportService {
  void transferAccounts(List<Account> accounts, BursarExportJob bursarFeeFines);

  /** for testing only */
  List<Account> getAllAccounts();
}
