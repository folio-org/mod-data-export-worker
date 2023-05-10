package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportFilter;
import org.folio.dew.domain.dto.BursarExportFilterAggregate;
import org.folio.dew.domain.dto.BursarExportFilterAggregate.ConditionEnum;
import org.folio.dew.domain.dto.BursarExportFilterAggregate.PropertyEnum;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

@Log4j2
public class BursarFilterAggregateEvaluatorTest {

  @Test
  void testEvaluateJSONNullableExportFilter() {
    AggregatedAccountsByUser aggregatedAccounts = AggregatedAccountsByUser
      .builder()
      .accounts(null)
      .user(null)
      .build();

    BursarExportFilterPass bursarExportFilterPass = new BursarExportFilterPass();

    JsonNullable<BursarExportFilter> jsonNullableFilterPass = JsonNullable.of(
      bursarExportFilterPass
    );

    assertThat(
      BursarFilterAggregateEvaluator.evaluate(
        aggregatedAccounts,
        jsonNullableFilterPass
      ),
      is(true)
    );

    assertThat(
      BursarFilterAggregateEvaluator.evaluate(
        aggregatedAccounts,
        JsonNullable.<BursarExportFilter>undefined()
      ),
      is(true)
    );
  }

  @Test
  void testFilterAggregatedAccountsByNumRows() {
    List<Account> accounts = new ArrayList<Account>();
    int numRows = 10;
    for (int i = 0; i < numRows; i++) {
      Account account = new Account();
      accounts.add(account);
    }

    AggregatedAccountsByUser aggregatedAccounts = AggregatedAccountsByUser
      .builder()
      .accounts(accounts)
      .user(null)
      .build();

    BursarExportFilterAggregate filter = new BursarExportFilterAggregate();
    filter.setProperty(PropertyEnum.NUM_ROWS);
    filter.setAmount(10);

    filter.setCondition(ConditionEnum.GREATER_THAN_EQUAL);
    assertThat(
      BursarFilterAggregateEvaluator.evaluateAggregate(
        aggregatedAccounts,
        filter
      ),
      is(true)
    );

    filter.setCondition(ConditionEnum.LESS_THAN);
    assertThat(
      BursarFilterAggregateEvaluator.evaluateAggregate(
        aggregatedAccounts,
        filter
      ),
      is(false)
    );
  }

  @Test
  void tesstFilterAggregatedAccountsByTotalAmount() {
    List<Account> accounts = new ArrayList<Account>();
    int numRows = 10;
    for (int i = 0; i < numRows; i++) {
      Account account = new Account();
      account.setAmount(new BigDecimal(100));
      accounts.add(account);
    }

    AggregatedAccountsByUser aggregatedAccounts = AggregatedAccountsByUser
      .builder()
      .accounts(accounts)
      .user(null)
      .build();

    BursarExportFilterAggregate filter = new BursarExportFilterAggregate();
    filter.setProperty(PropertyEnum.TOTAL_AMOUNT);
    filter.setAmount(1000);

    filter.setCondition(ConditionEnum.GREATER_THAN_EQUAL);
    assertThat(
      BursarFilterAggregateEvaluator.evaluateAggregate(
        aggregatedAccounts,
        filter
      ),
      is(true)
    );

    filter.setCondition(ConditionEnum.LESS_THAN);
    assertThat(
      BursarFilterAggregateEvaluator.evaluateAggregate(
        aggregatedAccounts,
        filter
      ),
      is(false)
    );
  }

  @Test
  void testCompareHelperMethod() {
    assertThat(
      BursarFilterAggregateEvaluator.compareHelper(
        ConditionEnum.LESS_THAN,
        8,
        10
      ),
      is(true)
    );
    assertThat(
      BursarFilterAggregateEvaluator.compareHelper(
        ConditionEnum.GREATER_THAN,
        14,
        10
      ),
      is(true)
    );
    assertThat(
      BursarFilterAggregateEvaluator.compareHelper(
        ConditionEnum.GREATER_THAN_EQUAL,
        10,
        10
      ),
      is(true)
    );
    assertThat(
      BursarFilterAggregateEvaluator.compareHelper(
        ConditionEnum.GREATER_THAN_EQUAL,
        12,
        10
      ),
      is(true)
    );
    assertThat(
      BursarFilterAggregateEvaluator.compareHelper(
        ConditionEnum.LESS_THAN_EQUAL,
        10,
        10
      ),
      is(true)
    );
    assertThat(
      BursarFilterAggregateEvaluator.compareHelper(
        ConditionEnum.LESS_THAN_EQUAL,
        7,
        10
      ),
      is(true)
    );
  }
}
