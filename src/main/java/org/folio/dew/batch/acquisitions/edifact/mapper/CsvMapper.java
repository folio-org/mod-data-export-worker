package org.folio.dew.batch.acquisitions.edifact.mapper;

import static java.util.stream.Collectors.groupingBy;
import static org.folio.dew.utils.Constants.LINE_BREAK;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.ClaimCsvConverter;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;

public class CsvMapper implements ExportResourceMapper {

  @Override
  public String convertForExport(List<CompositePurchaseOrder> compPOs, List<Piece> pieces, VendorEdiOrdersExportConfig ediExportConfig, String jobName) {
    var csvConverter = new ClaimCsvConverter();
    var csvResult = new StringBuilder(csvConverter.getCsvHeaders());
    getClaimEntries(compPOs, pieces).stream()
      .map(csvConverter::convertEntryToCsv)
      .map(line -> line.concat(LINE_BREAK))
      .forEachOrdered(csvResult::append);
    return csvResult.toString();
  }

  private static List<Pair<CompositePoLine, Piece>> getClaimEntries(List<CompositePurchaseOrder> orders, List<Piece> pieces) {
    var poLineIdToPieces = pieces.stream().collect(groupingBy(Piece::getPoLineId));
    return orders.stream()
      // 1. Get all composite PO lines
      .flatMap(order -> order.getCompositePoLines().stream()
        // 2. For each composite PO line, get all pieces
        .flatMap(poLine -> poLineIdToPieces.get(poLine.getId()).stream()
          // 3. Map each piece to a pair of composite PO line and piece
          .map(piece -> Pair.of(poLine, piece))))
      .toList();
  }

}
