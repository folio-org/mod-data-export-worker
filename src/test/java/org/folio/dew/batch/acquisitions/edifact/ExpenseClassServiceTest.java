package org.folio.dew.batch.acquisitions.edifact;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.edifact.client.ExpenseClassClient;
import org.folio.dew.batch.acquisitions.edifact.services.ExpenseClassService;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class ExpenseClassServiceTest extends BaseBatchTest {
  @Autowired
  private ExpenseClassService expenseClassService;
  @MockBean
  private ExpenseClassClient client;

  @Test
  void getExpenseClassCode() {
    String expenseClassCode = expenseClassService.getExpenseClassCode("1bcc3247-99bf-4dca-9b0f-7bc51a2998c2");
    assertEquals("", expenseClassCode);
  }

  @Test
  void getExpenseClassCodeFromJson() {
    Mockito.when(client.getExpenseClass(anyString()))
      .thenReturn(new JSONObject("{\"code\": \"Elec\"}"));
    String expenseClassCode = expenseClassService.getExpenseClassCode("1bcc3247-99bf-4dca-9b0f-7bc51a2998c2");
    assertEquals("Elec", expenseClassCode);
  }
}
