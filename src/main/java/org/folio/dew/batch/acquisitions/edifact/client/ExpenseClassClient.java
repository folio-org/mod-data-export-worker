package org.folio.dew.batch.acquisitions.edifact.client;

import org.json.JSONObject;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "finance")
public interface ExpenseClassClient
{
  @GetMapping(value = "/expense-classes/{expenseClassId}", produces = MediaType.APPLICATION_JSON_VALUE)
  JSONObject getExpenseClass(@PathVariable String expenseClassId);

}
