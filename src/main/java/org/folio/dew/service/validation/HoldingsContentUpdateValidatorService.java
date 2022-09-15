package org.folio.dew.service.validation;

import lombok.RequiredArgsConstructor;
import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.domain.dto.HoldingsContentUpdateCollection;
import org.folio.dew.error.ContentUpdateValidationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HoldingsContentUpdateValidatorService {
  private final HoldingsLocationUpdateValidator locationUpdateValidator;

  public boolean validateContentUpdateCollection(HoldingsContentUpdateCollection contentUpdateCollection) {
    return contentUpdateCollection.getHoldingsContentUpdates().stream()
      .allMatch(this::isValidContentUpdate);
  }

  private boolean isValidContentUpdate(HoldingsContentUpdate update) {
    return resolveValidator(update).isValid(update);
  }

  private ContentUpdateValidator<HoldingsContentUpdate> resolveValidator(HoldingsContentUpdate update) {
    switch (update.getOption()) {
    case TEMPORARY_LOCATION:
    case PERMANENT_LOCATION:
      return locationUpdateValidator;
    default:
      throw new ContentUpdateValidationException(update.getOption() + " update is not supported");
    }
  }
}
