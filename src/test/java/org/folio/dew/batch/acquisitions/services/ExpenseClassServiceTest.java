package org.folio.dew.batch.acquisitions.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.folio.dew.BaseBatchTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ExpenseClassServiceTest extends BaseBatchTest {
  @Autowired
  private ExpenseClassService expenseClassService;

  @BeforeAll
  static void beforeAll() {
    setUpTenant(NON_CONSORTIUM_TENANT);
  }

  @Test
  void getExpenseClassCode() {
    String expenseClassCode = expenseClassService.getExpenseClassCode("1bcc3247-99bf-4dca-9b0f-7bc51a2998c2");
    assertEquals("Elec", expenseClassCode);
  }

  @Test
  void shouldReturnEmptyString() {
    String expenseClassCode = expenseClassService.getExpenseClassCode("2bcc3250-99bf-4dyr-9b0f-9bc51a3098a4");
    assertEquals("", expenseClassCode);
  }
}
