package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

import static org.folio.dew.batch.acquisitions.edifact.utils.ExportUtils.getVendorAccountNumber;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportUtils.getVendorOrderNumber;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.Piece;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ClaimCsvFields implements ExtractableField<Pair<CompositePoLine, Piece>, String> {

  POL_NUMBER("POL number", pair -> pair.getKey().getPoLineNumber()),
  ORDER_NUMBER("Vendor order number", pair -> getVendorOrderNumber(pair.getKey())),
  ACCOUNT_NUMBER("Account number", pair -> getVendorAccountNumber(pair.getKey())),
  TITLE("Title from piece", pair -> "CHANGEME"), // FIXME: piece.titleId | poLine.titleOrPackage | title from inventory by piece.titleId
  DISPLAY_SUMMARY("Display summary", pair -> pair.getValue().getDisplaySummary()),
  CHRONOLOGY("Chronology", pair -> pair.getValue().getChronology()),
  ENUMERATION("Enumeration", pair -> pair.getValue().getEnumeration()),
  QUANTITY("Quantity", pair -> "CHANGEME"); // FIXME: implement quantity extraction

  @Getter
  private final String name;
  private final Function<Pair<CompositePoLine, Piece>, String> extractor;


  @Override
  public String extract(Pair<CompositePoLine, Piece> item) {
    return extractor.apply(item);
  }

}
