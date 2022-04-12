package org.folio.dew.controller;

import static org.folio.dew.domain.dto.ContentUpdate.ActionEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.ContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.ContentUpdate.OptionEnum.PERMANENT_LOCATION;
import static org.folio.dew.domain.dto.ContentUpdate.OptionEnum.TEMPORARY_LOCATION;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.dew.domain.dto.ContentUpdate;

@AllArgsConstructor
@Getter
public enum ItemsContentUpdateTestData {
  REPLACE_WITH_PERMANENT_LOCATION(PERMANENT_LOCATION, REPLACE_WITH, "Annex", "src/test/resources/output/expected_items_with_updated_permanent_location.json", "src/test/resources/output/expected_items_with_updated_permanent_location.csv"),
  REPLACE_WITH_TEMPORARY_LOCATION(TEMPORARY_LOCATION, REPLACE_WITH, "Annex", "src/test/resources/output/expected_items_with_updated_temporary_location.json", "src/test/resources/output/expected_items_with_updated_temporary_location.csv"),
  REPLACE_WITH_NULL_PERMANENT_LOCATION(PERMANENT_LOCATION, REPLACE_WITH, null, "src/test/resources/output/expected_items_with_deleted_permanent_location.json", "src/test/resources/output/expected_items_with_deleted_permanent_location.csv"),
  REPLACE_WITH_NULL_TEMPORARY_LOCATION(TEMPORARY_LOCATION, REPLACE_WITH, null, "src/test/resources/output/expected_items_with_deleted_temporary_location.json", "src/test/resources/output/expected_items_with_deleted_temporary_location.csv"),
  CLEAR_FIELD_PERMANENT_LOCATION(PERMANENT_LOCATION, CLEAR_FIELD, null, "src/test/resources/output/expected_items_with_deleted_permanent_location.json", "src/test/resources/output/expected_items_with_deleted_permanent_location.csv"),
  CLEAR_FIELD_TEMPORARY_LOCATION(TEMPORARY_LOCATION, CLEAR_FIELD, null, "src/test/resources/output/expected_items_with_deleted_temporary_location.json", "src/test/resources/output/expected_items_with_deleted_temporary_location.csv");

  final ContentUpdate.OptionEnum option;
  final ContentUpdate.ActionEnum action;
  final String value;
  final String expectedJsonPath;
  final String expectedCsvPath;
}
