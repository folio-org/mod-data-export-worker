package org.folio.dew.batch.acquisitions.mapper.converter;

import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getFormattedDate;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getVendorAccountNumber;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getVendorOrderNumber;

import java.util.function.Function;

import org.folio.dew.domain.dto.acquisitions.edifact.ClaimCsvEntry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ClaimCsvFields implements ExtractableField<ClaimCsvEntry, String> {

  POL_NUMBER("POL number", entry -> entry.poLine().getPoLineNumber()),
  ORDER_NUMBER("Vendor order number", entry -> getVendorOrderNumber(entry.poLine())),
  ACCOUNT_NUMBER("Account number", entry -> getVendorAccountNumber(entry.poLine())),
  EXPECTED_DATE("Expected date", entry -> getFormattedDate(entry.piece().getReceiptDate())),
  TITLE("Title from piece", ClaimCsvEntry::title),
  DISPLAY_SUMMARY("Display summary", entry -> entry.piece().getDisplaySummary()),
  CHRONOLOGY("Chronology", entry -> entry.piece().getChronology()),
  ENUMERATION("Enumeration", entry -> entry.piece().getEnumeration()),
  QUANTITY("Quantity", entry -> String.valueOf(entry.quantity())),
  EXTERNAL_NOTE("External note", entry -> entry.piece().getExternalNote());

  @Getter
  private final String name;
  private final Function<ClaimCsvEntry, String> extractor;


  @Override
  public String extract(ClaimCsvEntry entry) {
    return extractor.apply(entry);
  }

}
