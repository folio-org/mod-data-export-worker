package org.folio.dew.domain.dto.acquisitions.edifact;

import java.util.Objects;

import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.PoLine;

public record ClaimCsvEntry(PoLine poLine, Piece piece, String title, long quantity) {

  public ClaimCsvEntry withQuantity(long quantity) {
    return new ClaimCsvEntry(poLine, piece, title, quantity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClaimCsvEntry that = (ClaimCsvEntry) o;
    return Objects.equals(poLine.getPoLineNumber(), that.poLine.getPoLineNumber())
      && Objects.equals(piece.getDisplaySummary(), that.piece.getDisplaySummary())
      && Objects.equals(piece.getChronology(), that.piece.getChronology())
      && Objects.equals(piece.getEnumeration(), that.piece.getEnumeration())
      && Objects.equals(title, that.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(poLine.getPoLineNumber(), piece.getDisplaySummary(), piece.getChronology(), piece.getEnumeration(), title);
  }

}
