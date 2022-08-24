package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.ExpenseClassClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExpenseClassService {
  private final ExpenseClassClient expenseClassClient;

  private JsonNode getExpenseClass(String id) {
    return expenseClassClient.getExpenseClass(id);
  }

  @Cacheable(cacheNames = "expenseClasses")
  public String getExpenseClassCode(String id) {
    JsonNode jsonObject = getExpenseClass(id);
    String expenseClassCode = "";

    if (jsonObject != null && !jsonObject.get("code").asText().isEmpty()) {
      expenseClassCode = jsonObject.get("code").asText();
    }

    return expenseClassCode;
  }
}
