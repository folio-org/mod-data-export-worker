package org.folio.dew.controller;

import static org.folio.dew.domain.dto.ItemContentUpdate.ActionEnum.CLEAR_FIELD;
import static org.folio.dew.domain.dto.ItemContentUpdate.ActionEnum.REPLACE_WITH;
import static org.folio.dew.domain.dto.ItemContentUpdate.OptionEnum.PERMANENT_LOCATION;
import static org.folio.dew.domain.dto.ItemContentUpdate.OptionEnum.STATUS;
import static org.folio.dew.domain.dto.ItemContentUpdate.OptionEnum.TEMPORARY_LOCATION;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.dew.domain.dto.ItemContentUpdate;

@AllArgsConstructor
@Getter
public enum ItemsContentUpdateTestData {
  REPLACE_WITH_PERMANENT_LOCATION(PERMANENT_LOCATION, REPLACE_WITH, "Annex",
    "src/test/resources/output/expected_items_with_updated_permanent_location.json",
    "src/test/resources/output/expected_items_with_updated_permanent_location.csv",
    "src/test/resources/output/expected_preview_items_with_updated_permanent_location.csv"),
  REPLACE_WITH_TEMPORARY_LOCATION(TEMPORARY_LOCATION, REPLACE_WITH, "Annex",
    "src/test/resources/output/expected_items_with_updated_temporary_location.json",
    "src/test/resources/output/expected_items_with_updated_temporary_location.csv",
    "src/test/resources/output/expected_preview_items_with_updated_temporary_location.csv"),
  REPLACE_WITH_NULL_PERMANENT_LOCATION(PERMANENT_LOCATION, REPLACE_WITH, null,
    "src/test/resources/output/expected_items_with_deleted_permanent_location.json",
    "src/test/resources/output/expected_items_with_deleted_permanent_location.csv",
    "src/test/resources/output/expected_preview_items_with_deleted_permanent_location.csv"),
  REPLACE_WITH_NULL_TEMPORARY_LOCATION(TEMPORARY_LOCATION, REPLACE_WITH, null,
    "src/test/resources/output/expected_items_with_deleted_temporary_location.json",
    "src/test/resources/output/expected_items_with_deleted_temporary_location.csv",
    "src/test/resources/output/expected_preview_items_with_deleted_temporary_location.csv"),
  REPLACE_WITH_ALLOWED_STATUS(STATUS, REPLACE_WITH, "Missing",
    "src/test/resources/output/expected_items_with_updated_status.json",
    null, null),
  REPLACE_WITH_NOT_ALLOWED_STATUS(STATUS, REPLACE_WITH, "Aged to lost",
    "src/test/resources/output/expected_items_with_non_updated_status.json",
    null, null),
  REPLACE_WITH_EMPTY_STATUS(STATUS, REPLACE_WITH, null,
    "src/test/resources/output/expected_items_with_non_updated_status.json",
    null, null),
  CLEAR_FIELD_PERMANENT_LOCATION(PERMANENT_LOCATION, CLEAR_FIELD, null,
    "src/test/resources/output/expected_items_with_deleted_permanent_location.json",
    "src/test/resources/output/expected_items_with_deleted_permanent_location.csv",
    "src/test/resources/output/expected_preview_items_with_deleted_permanent_location.csv"),
  CLEAR_FIELD_TEMPORARY_LOCATION(TEMPORARY_LOCATION, CLEAR_FIELD, null,
    "src/test/resources/output/expected_items_with_deleted_temporary_location.json",
    "src/test/resources/output/expected_items_with_deleted_temporary_location.csv",
    "src/test/resources/output/expected_preview_items_with_deleted_temporary_location.csv"),
  CLEAR_FIELD_STATUS(STATUS, CLEAR_FIELD, null,
    "src/test/resources/output/expected_items_with_non_updated_status.json",
    null, null);

  final ItemContentUpdate.OptionEnum option;
  final ItemContentUpdate.ActionEnum action;
  final String value;
  final String expectedPreviewJsonPath;
  final String expectedCsvPath;
  final String expectedPreviewCsvPath;
}
