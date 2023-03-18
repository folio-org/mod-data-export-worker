package org.folio.dew.batch.bursarfeesfines.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.domain.dto.BursarExportFilter;
import org.folio.dew.domain.dto.BursarExportFilterAge;
import org.folio.dew.domain.dto.BursarExportFilterAmount;
import org.folio.dew.domain.dto.BursarExportFilterCampus;
import org.folio.dew.domain.dto.BursarExportFilterCondition;
import org.folio.dew.domain.dto.BursarExportFilterFeeType;
import org.folio.dew.domain.dto.BursarExportFilterInstitution;
import org.folio.dew.domain.dto.BursarExportFilterLibrary;
import org.folio.dew.domain.dto.BursarExportFilterLocation;
import org.folio.dew.domain.dto.BursarExportFilterNegation;
import org.folio.dew.domain.dto.BursarExportFilterPatronGroup;
import org.folio.dew.domain.dto.BursarExportFilterServicePoint;
import org.folio.dew.domain.dto.bursarfeesfines.AccountWithAncillaryData;
import org.springframework.cglib.core.Local;

@Log4j2
@UtilityClass
public class BursarFilterEvaluator {

  public static boolean evaluate(
    AccountWithAncillaryData account,
    BursarExportFilter filter
  ) {
    if (filter instanceof BursarExportFilterAge) {
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
      return true;
    } else if (filter instanceof BursarExportFilterCampus) {
      BursarExportFilterCampus filterCampus = (BursarExportFilterCampus) filter;
      return true;
    } else if (filter instanceof BursarExportFilterFeeType) {
      BursarExportFilterFeeType filterFeeType = (BursarExportFilterFeeType) filter;
      return true;
    } else if (filter instanceof BursarExportFilterInstitution) {
      BursarExportFilterInstitution filterInstitution = (BursarExportFilterInstitution) filter;
      return true;
    } else if (filter instanceof BursarExportFilterLibrary) {
      BursarExportFilterLibrary filterLibrary = (BursarExportFilterLibrary) filter;
      return true;
    } else if (filter instanceof BursarExportFilterLocation) {
      BursarExportFilterLocation filterLocation = (BursarExportFilterLocation) filter;
      return true;
    } else if (filter instanceof BursarExportFilterPatronGroup) {
      BursarExportFilterPatronGroup filterPatronGroup = (BursarExportFilterPatronGroup) filter;
      log.info(UUID.fromString(account.getUser().getPatronGroup()));
      return (
        UUID
          .fromString(account.getUser().getPatronGroup())
          .equals(filterPatronGroup.getPatronGroupId())
      );
    } else if (filter instanceof BursarExportFilterServicePoint) {
      BursarExportFilterServicePoint filterServicePoint = (BursarExportFilterServicePoint) filter;
      return true;
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
