package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.Piece;

public class ClaimCsvConverter extends AbstractCsvConverter<Pair<CompositePoLine, Piece>> {

  @Override
  protected ExtractableField<Pair<CompositePoLine, Piece>, String>[] getFields() {
    return ClaimCsvFields.values();
  }

  @Override
  protected String[] getHeaders() {
    return Arrays.stream(ClaimCsvFields.values())
      .map(ClaimCsvFields::getName)
      .toArray(String[]::new);
  }

}
