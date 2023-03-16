package org.folio.dew.batch.bursarfeesfines.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportJob;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.User;

public interface BursarExportService {
  void transferAccounts(List<Account> accounts, BursarExportJob bursarFeeFines);

  List<Account> getAllAccounts();

  Map<String, User> getUsers(Set<String> userIds);
  Map<String, Item> getItems(Set<String> itemIds);
}
