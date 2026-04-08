package org.folio.dew.batch.acquisitions.mapper;

import static org.folio.dew.utils.Constants.LINE_BREAK;

import java.util.List;

import org.folio.dew.batch.acquisitions.mapper.converter.CsvConverter;
import org.folio.dew.batch.acquisitions.mapper.converter.ExtractableField;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;

public abstract class AbstractCsvMapper<T> implements ExportResourceMapper {

  private final CsvConverter<T> converter;

  protected AbstractCsvMapper(ExtractableField<T, String>[] fields) {
    this.converter = new CsvConverter<>(fields);
  }

  @Override
  public String convertForExport(List<CompositePurchaseOrder> compPOs, List<Piece> pieces,
                                 VendorEdiOrdersExportConfig ediExportConfig, String jobName) {
    var csvResult = new StringBuilder(converter.getCsvHeaders()).append(LINE_BREAK);
    getEntries(compPOs, pieces).stream()
      .map(converter::convertEntryToCsv)
      .map(line -> line.concat(LINE_BREAK))
      .forEachOrdered(csvResult::append);
    return csvResult.toString();
  }

  protected abstract List<T> getEntries(List<CompositePurchaseOrder> compPOs, List<Piece> pieces);

}