package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.batch.acquisitions.edifact.client.ExpenseClassClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseClassService {
  @Autowired
  private final ExpenseClassClient expenseClassClient;

  private JSONObject getExpenseClass(String id) {
    return expenseClassClient.getExpenseClass(id);
  }

  @Cacheable(cacheNames = "expense-classes")
  public String getExpenseClassCode(String id) {
    JSONObject jsonObject = getExpenseClass(id);
    String expenseClassCode = "";

    if (!jsonObject.isEmpty() && jsonObject.getString("code") != null) {
      expenseClassCode = jsonObject.getString("code");
    }

    return expenseClassCode;
  }
}
