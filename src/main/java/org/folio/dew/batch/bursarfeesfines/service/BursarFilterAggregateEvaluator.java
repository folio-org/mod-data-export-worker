package org.folio.dew.batch.bursarfeesfines.service;

import io.micrometer.common.lang.NonNull;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportFilterAggregate;
import org.folio.dew.domain.dto.BursarExportFilterAggregate.ConditionEnum;
import org.folio.dew.domain.dto.BursarExportFilterAggregate.PropertyEnum;
import org.folio.dew.domain.dto.bursarfeesfines.AggregatedAccountsByUser;
import org.openapitools.jackson.nullable.JsonNullable;

@Log4j2
@UtilityClass
public class BursarFilterAggregateEvaluator {

  public static boolean evaluate(
    AggregatedAccountsByUser aggregatedAccounts,
    JsonNullable<BursarExportFilterAggregate> filter
  ) {
    if (filter.isPresent()) {
      return evaluate(aggregatedAccounts, filter.get());
    }
    return true;
  }

  public static boolean evaluate(
    AggregatedAccountsByUser aggregatedAccounts,
    @NonNull BursarExportFilterAggregate filter
  ) {
    int numRows = aggregatedAccounts.getAccounts().size();
    if (filter.getProperty() == PropertyEnum.NUM_ROWS) {
      return compareHelper(filter.getCondition(), numRows, filter.getAmount());
    } else if (filter.getProperty() == PropertyEnum.TOTAL_AMOUNT) {
      BigDecimal totalAmount = new BigDecimal(0);
      for (int i = 0; i < numRows; ++i) {
        totalAmount =
          totalAmount.add(aggregatedAccounts.getAccounts().get(i).getAmount());
      }
      return compareHelper(
        filter.getCondition(),
        totalAmount.intValue(),
        filter.getAmount()
      );
    } else {
      log.error("Unexpected aggregate filter {}", filter);
      return true;
    }
  }

  // TODO: determine if float is the desired type here?
  private final boolean compareHelper(
    ConditionEnum condition,
    int amount,
    int conditionAmount
  ) {
    switch (condition) {
      case LESS_THAN:
        return amount < conditionAmount;
      case GREATER_THAN:
        return amount > conditionAmount;
      case LESS_THAN_EQUAL:
        return amount <= conditionAmount;
      case GREATER_THAN_EQUAL:
        return amount >= conditionAmount;
      default:
        log.error("Unexpected aggregated filter condition {}", condition);
        return false;
    }
  }
}
