package org.folio.dew.service.update;

import static org.apache.commons.lang3.ObjectUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.domain.dto.HoldingsFormat;
import org.springframework.stereotype.Component;

@Component
public class HoldingsLocationUpdateStrategy implements UpdateStrategy<HoldingsFormat, HoldingsContentUpdate> {
  @Override
  public HoldingsFormat applyUpdate(HoldingsFormat holdingsFormat, HoldingsContentUpdate update) {
    switch (update.getAction()) {
    case REPLACE_WITH:
      return replaceLocation(holdingsFormat, update);
    case CLEAR_FIELD:
      return clearLocation(holdingsFormat);
    default:
      return holdingsFormat;
    }
  }

  private HoldingsFormat replaceLocation(HoldingsFormat holdingsFormat, HoldingsContentUpdate update) {
    var newLocation = update.getValue().toString();
    switch (update.getOption()) {
      case PERMANENT_LOCATION:
        return holdingsFormat
          .withPermanentLocation(newLocation);
      case TEMPORARY_LOCATION:
        return holdingsFormat
          .withTemporaryLocation(newLocation);
      default:
        return holdingsFormat;
    }
  }

  private HoldingsFormat clearLocation(HoldingsFormat holdingsFormat) {
    return holdingsFormat
      .withTemporaryLocation(EMPTY);
  }
}
