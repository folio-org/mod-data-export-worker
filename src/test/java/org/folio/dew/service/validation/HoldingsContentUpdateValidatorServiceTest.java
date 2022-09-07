package org.folio.dew.service.validation;

import static org.folio.dew.domain.dto.HoldingsContentUpdate.ActionEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.OptionEnum.PERMANENT_LOCATION;
import static org.folio.dew.domain.dto.HoldingsContentUpdate.OptionEnum.TEMPORARY_LOCATION;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.domain.dto.HoldingsContentUpdate;
import org.folio.dew.domain.dto.HoldingsContentUpdateCollection;
import org.folio.dew.error.ContentUpdateValidationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

class HoldingsContentUpdateValidatorServiceTest extends BaseBatchTest {
  @Autowired
  private HoldingsContentUpdateValidatorService validatorService;

  @ParameterizedTest
  @EnumSource(HoldingsContentUpdateValidTestData.class)
  void shouldAllowValidContentUpdateData(HoldingsContentUpdateValidTestData testData) {
    assertTrue(validatorService.validateContentUpdateCollection(testData.getUpdateCollection()));
  }

  @ParameterizedTest
  @EnumSource(HoldingsContentUpdateInvalidTestData.class)
  void shouldRejectInvalidContentUpdateData(HoldingsContentUpdateInvalidTestData testData) {
    assertThrows(ContentUpdateValidationException.class, () -> validatorService.validateContentUpdateCollection(testData.getUpdateCollection()));
  }

  @AllArgsConstructor
  @Getter
  enum HoldingsContentUpdateValidTestData {
    REPLACE_WITH_PERMANENT_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(PERMANENT_LOCATION)
        .action(REPLACE_WITH)
        .value("Annex")))
      .totalRecords(1)),
    REPLACE_WITH_TEMPORARY_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(TEMPORARY_LOCATION)
        .action(REPLACE_WITH)
        .value("Annex")))
      .totalRecords(1)),
    CLEAR_FIELD_TEMPORARY_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(TEMPORARY_LOCATION)
        .action(CLEAR_FIELD)))
      .totalRecords(1)),
    REPLACE_PERMANENT_LOCATION_CLEAR_TEMPORARY_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(List.of(new HoldingsContentUpdate()
        .option(PERMANENT_LOCATION)
        .action(REPLACE_WITH)
          .value("Annex"),
        new HoldingsContentUpdate()
        .option(TEMPORARY_LOCATION)
        .action(CLEAR_FIELD)))
      .totalRecords(2));

    final HoldingsContentUpdateCollection updateCollection;
  }

  @AllArgsConstructor
  @Getter
  enum HoldingsContentUpdateInvalidTestData {
    REPLACE_WITH_NON_EXISTING_PERMANENT_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(PERMANENT_LOCATION)
        .action(REPLACE_WITH)
        .value("Abc")))
      .totalRecords(1)),
    REPLACE_WITH_NON_EXISTING_TEMPORARY_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(TEMPORARY_LOCATION)
        .action(REPLACE_WITH)
        .value("Abc")))
      .totalRecords(1)),
    CLEAR_FIELD_PERMANENT_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(Collections.singletonList(new HoldingsContentUpdate()
        .option(PERMANENT_LOCATION)
        .action(CLEAR_FIELD)))
      .totalRecords(1)),
    REPLACE_TEMPORARY_LOCATION_CLEAR_PERMANENT_LOCATION(new HoldingsContentUpdateCollection()
      .holdingsContentUpdates(List.of(new HoldingsContentUpdate()
          .option(TEMPORARY_LOCATION)
          .action(REPLACE_WITH)
          .value("Annex"),
        new HoldingsContentUpdate()
          .option(PERMANENT_LOCATION)
          .action(CLEAR_FIELD)))
      .totalRecords(2));

    final HoldingsContentUpdateCollection updateCollection;
  }
}


