package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

import java.util.Arrays;

import org.folio.dew.domain.dto.acquisitions.edifact.ClaimCsvEntry;

public class ClaimCsvConverter extends AbstractCsvConverter<ClaimCsvEntry> {

  @Override
  protected ExtractableField<ClaimCsvEntry, String>[] getFields() {
    return ClaimCsvFields.values();
  }

  @Override
  protected String[] getHeaders() {
    return Arrays.stream(ClaimCsvFields.values())
      .map(ClaimCsvFields::getName)
      .toArray(String[]::new);
  }

}
