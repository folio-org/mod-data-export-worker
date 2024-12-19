package org.folio.dew.domain.dto.acquisitions.edifact;

import java.util.Objects;

import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.Piece;

public record ClaimCsvEntry(CompositePoLine compositePoLine, Piece piece, String title, long quantity) {

  public ClaimCsvEntry withQuantity(long quantity) {
    return new ClaimCsvEntry(compositePoLine, piece, title, quantity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClaimCsvEntry that = (ClaimCsvEntry) o;
    return Objects.equals(compositePoLine.getPoLineNumber(), that.compositePoLine.getPoLineNumber())
      && Objects.equals(piece.getDisplaySummary(), that.piece.getDisplaySummary())
      && Objects.equals(piece.getChronology(), that.piece.getChronology())
      && Objects.equals(piece.getEnumeration(), that.piece.getEnumeration())
      && Objects.equals(title, that.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(compositePoLine.getPoLineNumber(), piece.getDisplaySummary(), piece.getChronology(), piece.getEnumeration(), title);
  }

}
