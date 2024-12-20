package org.folio.dew.batch.acquisitions.edifact.mapper.converter;

import static org.folio.dew.batch.acquisitions.edifact.utils.ExportUtils.getVendorAccountNumber;
import static org.folio.dew.batch.acquisitions.edifact.utils.ExportUtils.getVendorOrderNumber;

import java.util.function.Function;

import org.folio.dew.domain.dto.acquisitions.edifact.ClaimCsvEntry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ClaimCsvFields implements ExtractableField<ClaimCsvEntry, String> {

  POL_NUMBER("POL number", entry -> entry.compositePoLine().getPoLineNumber()),
  ORDER_NUMBER("Vendor order number", entry -> getVendorOrderNumber(entry.compositePoLine())),
  ACCOUNT_NUMBER("Account number", entry -> getVendorAccountNumber(entry.compositePoLine())),
  TITLE("Title from piece", ClaimCsvEntry::title),
  DISPLAY_SUMMARY("Display summary", entry -> entry.piece().getDisplaySummary()),
  CHRONOLOGY("Chronology", entry -> entry.piece().getChronology()),
  ENUMERATION("Enumeration", entry -> entry.piece().getEnumeration()),
  QUANTITY("Quantity", entry -> String.valueOf(entry.quantity()));

  @Getter
  private final String name;
  private final Function<ClaimCsvEntry, String> extractor;


  @Override
  public String extract(ClaimCsvEntry entry) {
    return extractor.apply(entry);
  }

}
