package org.folio.dew.service.validation;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.OptionEnum.PERMANENT_LOCATION;

import lombok.RequiredArgsConstructor;
import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.error.BulkEditException;
import org.folio.dew.error.ContentUpdateValidationException;
import org.folio.dew.service.HoldingsReferenceService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HoldingsLocationUpdateValidator implements ContentUpdateValidator<HoldingsContentUpdate> {
  private final HoldingsReferenceService referenceService;
  @Override
  public boolean isValid(HoldingsContentUpdate update) {
    String errorMessage = null;
    if (REPLACE_WITH == update.getAction()) {
      try {
        if (isNull(update.getValue())) {
          errorMessage = "Location name cannot be empty";
        }
        var locationName = update.getValue().toString();
        referenceService.getLocationByName(locationName);
      } catch (BulkEditException e) {
        errorMessage = "Location does not exist";
      }
    } else if (PERMANENT_LOCATION == update.getOption()) {
      errorMessage = "Permanent location cannot be cleared";
    }
    if (nonNull(errorMessage)) {
      throw new ContentUpdateValidationException(errorMessage);
    }
    return true;
  }
}
