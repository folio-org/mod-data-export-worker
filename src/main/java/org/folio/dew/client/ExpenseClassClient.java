package org.folio.dew.client;

import org.folio.dew.domain.dto.acquisitions.edifact.ExpenseClass;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "finance")
public interface ExpenseClassClient {
  @GetExchange(value = "/expense-classes/{expenseClassId}", accept = MediaType.APPLICATION_JSON_VALUE)
  ExpenseClass getExpenseClass(@PathVariable String expenseClassId);

}
