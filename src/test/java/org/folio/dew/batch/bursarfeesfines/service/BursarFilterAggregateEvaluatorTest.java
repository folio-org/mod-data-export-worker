package org.folio.dew.batch.bursarfeesfines.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.Account;
import org.folio.dew.domain.dto.BursarExportFilter;
import org.folio.dew.domain.dto.BursarExportFilterAggregate;
import org.folio.dew.domain.dto.BursarExportFilterAggregate.ConditionEnum;
import org.folio.dew.domain.dto.BursarExportFilterAggregate.PropertyEnum;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportFilterPatronGroup;
import org.folio.dew.domain.dto.BursarExportFilterServicePoint;
import org.folio.dew.domain.dto.User;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

@Log4j2
public class BursarFilterAggregateEvaluatorTest {

  public static AggregatedAccountsByUser aggregatedAccounts;

  @BeforeAll
  static void beforeAll() {
    List<Account> accounts = new ArrayList<Account>();
    for (int i = 0; i < 10; i++) {
      Account account = new Account();
      account.setAmount(new BigDecimal(100));
      accounts.add(account);
    }

    User user = new User();
    user.setPatronGroup("0000-00-00-00-000000");

    aggregatedAccounts =
      AggregatedAccountsByUser.builder().accounts(accounts).user(user).build();
  }

  @Test
  void testEvaluateJSONNullableExportFilter() {
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
  void testEvaluateFilterByPatronGroup() {
    BursarExportFilterPatronGroup bursarExportFilterPatronGroup = new BursarExportFilterPatronGroup();
    bursarExportFilterPatronGroup.setPatronGroupId(
      UUID.fromString("0000-00-00-00-000000")
    );

    JsonNullable<BursarExportFilter> jsonNullableFilterPatronGroup = JsonNullable.of(
      bursarExportFilterPatronGroup
    );

    assertThat(
      BursarFilterAggregateEvaluator.evaluate(
        aggregatedAccounts,
        jsonNullableFilterPatronGroup
      ),
      is(true)
    );

    assertThat(
      BursarFilterAggregateEvaluator.evaluate(
        aggregatedAccounts,
        JsonNullable.of(new BursarExportFilterServicePoint())
      ),
      is(true)
    );
  }

  @Test
  void testFilterAggregatedAccountsByNumRows() {
    BursarExportFilterAggregate filter = new BursarExportFilterAggregate();
    filter.setAmount(10);

    // test null filter
    assertThat(
      BursarFilterAggregateEvaluator.evaluateAggregate(
        aggregatedAccounts,
        null
      ),
      is(true)
    );

    // test invalid filter
    assertThat(
      BursarFilterAggregateEvaluator.evaluateAggregate(
        aggregatedAccounts,
        filter
      ),
      is(true)
    );

    filter.setProperty(PropertyEnum.NUM_ROWS);
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
  void testFilterAggregatedAccountsByTotalAmount() {
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
    assertThat(
      BursarFilterAggregateEvaluator.compareHelper(null, 10, 10),
      is(true)
    );
  }
}
