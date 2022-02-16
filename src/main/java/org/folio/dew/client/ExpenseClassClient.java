package org.folio.dew.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.fasterxml.jackson.databind.JsonNode;

@FeignClient(name = "finance")
public interface ExpenseClassClient {
  @GetMapping(value = "/expense-classes/{expenseClassId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JsonNode getExpenseClass(@PathVariable String expenseClassId);

}
