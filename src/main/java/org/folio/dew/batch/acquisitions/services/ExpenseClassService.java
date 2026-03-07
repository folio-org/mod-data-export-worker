package org.folio.dew.batch.acquisitions.services;

import org.folio.dew.client.ExpenseClassClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class ExpenseClassService {
  private final ExpenseClassClient expenseClassClient;

  @Cacheable(cacheNames = "expenseClasses")
  public String getExpenseClassCode(String id) {
    var expenseClass = expenseClassClient.getExpenseClass(id);
    return isNull(expenseClass) ? "" : expenseClass.getCode();
  }
}
