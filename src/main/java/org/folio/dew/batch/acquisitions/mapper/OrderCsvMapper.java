package org.folio.dew.batch.acquisitions.mapper;

import static org.folio.dew.utils.Constants.LINE_BREAK;

import java.util.List;

import org.folio.dew.batch.acquisitions.mapper.converter.OrderCsvConverter;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.OrderCsvEntry;

public class OrderCsvMapper implements ExportResourceMapper {

  @Override
  public String convertForExport(List<CompositePurchaseOrder> compPOs, List<Piece> pieces,
                                 VendorEdiOrdersExportConfig ediExportConfig, String jobName) {
    var converter = new OrderCsvConverter();
    var csvResult = new StringBuilder(converter.getCsvHeaders()).append(LINE_BREAK);
    compPOs.stream()
      .flatMap(order -> order.getPoLines().stream()
        .map(poLine -> new OrderCsvEntry(poLine, order)))
      .map(converter::convertEntryToCsv)
      .map(line -> line.concat(LINE_BREAK))
      .forEachOrdered(csvResult::append);
    return csvResult.toString();
  }

}
