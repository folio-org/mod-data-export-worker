package org.folio.dew.service.update;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.ActionEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.OptionEnum.PERMANENT_LOCATION;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.OptionEnum.TEMPORARY_LOCATION;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.domain.dto.HoldingsFormat;

@AllArgsConstructor
@Getter
public enum HoldingsLocationsTestData {
  CLEAR_TEMPORARY_LOCATION(
    HoldingsFormat.builder().permanentLocation("permanent location").temporaryLocation("temporary location").build(),
    new HoldingsContentUpdate().option(TEMPORARY_LOCATION).action(CLEAR_FIELD),
    HoldingsFormat.builder().permanentLocation("permanent location").temporaryLocation(EMPTY).effectiveLocation("permanent location").build()
  ),
  REPLACE_TEMPORARY_LOCATION(
    HoldingsFormat.builder().permanentLocation("permanent location").temporaryLocation("temporary location").build(),
    new HoldingsContentUpdate().option(TEMPORARY_LOCATION).action(REPLACE_WITH).value("new location"),
    HoldingsFormat.builder().permanentLocation("permanent location").temporaryLocation("new location").effectiveLocation("new location").build()
  ),
  REPLACE_PERMANENT_LOCATION_EMPTY_TEMPORARY_LOCATION(
    HoldingsFormat.builder().permanentLocation("permanent location").build(),
    new HoldingsContentUpdate().option(PERMANENT_LOCATION).action(REPLACE_WITH).value("new location"),
    HoldingsFormat.builder().permanentLocation("new location").effectiveLocation("new location").build()
  ),
  REPLACE_PERMANENT_LOCATION_NON_EMPTY_TEMPORARY_LOCATION(
    HoldingsFormat.builder().permanentLocation("permanent location").temporaryLocation("temporary location").build(),
    new HoldingsContentUpdate().option(PERMANENT_LOCATION).action(REPLACE_WITH).value("new location"),
    HoldingsFormat.builder().permanentLocation("new location").temporaryLocation("temporary location").effectiveLocation("temporary location").build()
  );

  private HoldingsFormat holdingsFormat;
  private HoldingsContentUpdate update;
  private HoldingsFormat expectedHoldingsFormat;
}
