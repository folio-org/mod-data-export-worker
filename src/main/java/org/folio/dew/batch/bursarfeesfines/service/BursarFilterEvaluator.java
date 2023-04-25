package org.folio.dew.batch.bursarfeesfines.service;

import jakarta.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    } else if (filter instanceof BursarExportFilterAge) {
      BursarExportFilterAge filterAge = (BursarExportFilterAge) filter;
      LocalDate currentDate = LocalDate.now();

      if (account.getAccount().getDateCreated() == null) {
        return true;
      }
      return (
        ChronoUnit.DAYS.between(
          account.getAccount().getDateCreated().toInstant(),
          currentDate
        ) >
        filterAge.getNumDays()
      );
    } else if (filter instanceof BursarExportFilterAmount) {
      BursarExportFilterAmount filterAmount = (BursarExportFilterAmount) filter;
      int centFeeValue = account
        .getAccount()
        .getAmount()
        .multiply(new BigDecimal("100"))
        .intValue();

      switch (filterAmount.getCondition()) {
        case LESS_THAN:
          return centFeeValue < filterAmount.getAmount();
        case GREATER_THAN:
          return centFeeValue > filterAmount.getAmount();
        case LESS_THAN_EQUAL:
          return centFeeValue <= filterAmount.getAmount();
        case GREATER_THAN_EQUAL:
          return centFeeValue >= filterAmount.getAmount();
        default:
          return false;
      }
    } else if (filter instanceof BursarExportFilterFeeType) {
      BursarExportFilterFeeType filterFeeType = (BursarExportFilterFeeType) filter;
      return UUID
        .fromString(account.getAccount().getFeeFineId())
        .equals(filterFeeType.getFeeFineTypeId());
    } else if (filter instanceof BursarExportFilterFeeFineOwner){
        BursarExportFilterFeeFineOwner filterFeeFineOwner = (BursarExportFilterFeeFineOwner) filter;
        return UUID
          .fromString(account.getAccount().getFeeFineOwner())
          .equals(filterFeeFineOwner.getFeeFineOwnerId());
    } else if (filter instanceof BursarExportFilterLocation) {
      BursarExportFilterLocation filterLocation = (BursarExportFilterLocation) filter;
      return UUID
        .fromString(account.getItem().getEffectiveLocation().getId())
        .equals(filterLocation.getLocationId());
    } else if (filter instanceof BursarExportFilterPatronGroup) {
      BursarExportFilterPatronGroup filterPatronGroup = (BursarExportFilterPatronGroup) filter;
      return (
        UUID
          .fromString(account.getUser().getPatronGroup())
          .equals(filterPatronGroup.getPatronGroupId())
      );
    } else if (filter instanceof BursarExportFilterServicePoint) {
      BursarExportFilterServicePoint filterServicePoint = (BursarExportFilterServicePoint) filter;
      return UUID
        .fromString(account.getItem().getInTransitDestinationServicePointId())
        .equals(filterServicePoint.getServicePointId());
    } else if (filter instanceof BursarExportFilterCondition) {
      return evaluateCondition(account, (BursarExportFilterCondition) filter);
    } else if (filter instanceof BursarExportFilterNegation) {
      return !evaluate(account, (BursarExportFilterNegation) filter);
    } else {
      log.error("Unexpected filter: {}", filter);
      return true;
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
