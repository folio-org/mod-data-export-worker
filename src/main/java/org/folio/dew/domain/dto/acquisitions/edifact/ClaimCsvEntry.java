package org.folio.dew.domain.dto.acquisitions.edifact;

import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.Piece;

public record ClaimCsvEntry(CompositePoLine compositePoLine, Piece piece, String title, int quantity) {

}
