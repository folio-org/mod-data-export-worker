package org.folio.dew.batch.bursarfeesfines.service;

import jakarta.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportFilter;
import org.folio.dew.domain.dto.BursarExportFilterAge;
import org.folio.dew.domain.dto.BursarExportFilterAmount;
import org.folio.dew.domain.dto.BursarExportFilterCondition;
import org.folio.dew.domain.dto.BursarExportFilterFeeFineOwner;
import org.folio.dew.domain.dto.BursarExportFilterFeeType;
import org.folio.dew.domain.dto.BursarExportFilterLocation;
import org.folio.dew.domain.dto.BursarExportFilterNegation;
import org.folio.dew.domain.dto.BursarExportFilterPass;
import org.folio.dew.domain.dto.BursarExportFilterPatronGroup;
import org.folio.dew.domain.dto.BursarExportFilterServicePoint;
import org.folio.dew.domain.dto.Item;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.openapitools.jackson.nullable.JsonNullable;

@Log4j2
@UtilityClass
public class BursarFilterEvaluator {

  public static boolean evaluate(
    AccountWithAncillaryData account,
    JsonNullable<BursarExportFilter> filter
  ) {
    if (filter.isPresent()) {
      return evaluate(account, filter.get());
    }
    return true;
  }

  public static boolean evaluate(
    AccountWithAncillaryData account,
    @Nonnull BursarExportFilter filter
  ) {
    if (filter instanceof BursarExportFilterPass) {
      return true;
    } else if (filter instanceof BursarExportFilterAge filterAge) {
      if (account.getAccount().getDateCreated() == null) {
        return true;
      }
      return evaluateFilterAge(account, filterAge);
    } else if (filter instanceof BursarExportFilterAmount filterAmount) {
      return evaluateFilterAmount(account, filterAmount);
    } else if (filter instanceof BursarExportFilterFeeType filterFeeType) {
      return UUID
        .fromString(account.getAccount().getFeeFineId())
        .equals(filterFeeType.getFeeFineTypeId());
    } else if (
      filter instanceof BursarExportFilterFeeFineOwner filterFeeFineOwner
    ) {
      return UUID
        .fromString(account.getAccount().getOwnerId())
        .equals(filterFeeFineOwner.getFeeFineOwner());
    } else if (filter instanceof BursarExportFilterLocation filterLocation) {
      Item item = account.getItem();
      if (item == null) {
        return false;
      }
      return UUID
        .fromString(item.getEffectiveLocation().getId())
        .equals(filterLocation.getLocationId());
    } else if (
      filter instanceof BursarExportFilterPatronGroup filterPatronGroup
    ) {
      return (
        UUID
          .fromString(account.getUser().getPatronGroup())
          .equals(filterPatronGroup.getPatronGroupId())
      );
    } else if (
      filter instanceof BursarExportFilterServicePoint filterServicePoint
    ) {
      Item item = account.getItem();
      if (item == null) {
        return false;
      }
      return UUID
        .fromString(item.getInTransitDestinationServicePointId())
        .equals(filterServicePoint.getServicePointId());
    } else if (filter instanceof BursarExportFilterCondition filterCondition) {
      return evaluateCondition(account, filterCondition);
    } else if (filter instanceof BursarExportFilterNegation filterNegation) {
      return !evaluate(account, filterNegation.getCriteria());
    } else {
      log.error("Unexpected filter: {}", filter);
      return true;
    }
  }

  private static boolean evaluateFilterAge(
    AccountWithAncillaryData account,
    BursarExportFilterAge filter
  ) {
    int numDaysFilter = filter.getNumDays();
    long accountAge = ChronoUnit.DAYS.between(
      account.getAccount().getDateCreated().toInstant(),
      Instant.now()
    );

    switch (filter.getCondition()) {
      case LESS_THAN:
        return accountAge < numDaysFilter;
      case GREATER_THAN:
        return accountAge > numDaysFilter;
      case LESS_THAN_EQUAL:
        return accountAge <= numDaysFilter;
      case GREATER_THAN_EQUAL:
        return accountAge >= numDaysFilter;
      default:
        return false;
    }
  }

  private static boolean evaluateFilterAmount(
    AccountWithAncillaryData account,
    BursarExportFilterAmount filter
  ) {
    int centFeeValue = account
      .getAccount()
      .getAmount()
      .multiply(new BigDecimal("100"))
      .intValue();

    switch (filter.getCondition()) {
      case LESS_THAN:
        return centFeeValue < filter.getAmount();
      case GREATER_THAN:
        return centFeeValue > filter.getAmount();
      case LESS_THAN_EQUAL:
        return centFeeValue <= filter.getAmount();
      case GREATER_THAN_EQUAL:
        return centFeeValue >= filter.getAmount();
      default:
        return false;
    }
  }

  private static boolean evaluateCondition(
    AccountWithAncillaryData account,
    BursarExportFilterCondition filter
  ) {
    if (
      filter.getOperation() == BursarExportFilterCondition.OperationEnum.AND
    ) {
      return filter
        .getCriteria()
        .stream()
        .allMatch(subFilter -> evaluate(account, subFilter));
    } else {
      return filter
        .getCriteria()
        .stream()
        .anyMatch(subFilter -> evaluate(account, subFilter));
    }
  }
}
