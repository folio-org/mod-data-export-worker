package org.folio.dew.batch.acquisitions.edifact.mapper;

import static java.util.stream.Collectors.groupingBy;
import static org.folio.dew.utils.Constants.LINE_BREAK;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.ClaimCsvConverter;
import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.ClaimCsvEntry;

public class CsvMapper implements ExportResourceMapper {

  private final OrdersService ordersService;

  public CsvMapper(OrdersService ordersService) {
    this.ordersService = ordersService;
  }

  @Override
  public String convertForExport(List<CompositePurchaseOrder> compPOs, List<Piece> pieces, VendorEdiOrdersExportConfig ediExportConfig, String jobName) {
    var claimCsvConverter = new ClaimCsvConverter();
    var csvResult = new StringBuilder(claimCsvConverter.getCsvHeaders()).append(LINE_BREAK);
    getClaimEntries(compPOs, pieces).stream()
      .map(claimCsvConverter::convertEntryToCsv)
      .map(line -> line.concat(LINE_BREAK))
      .forEachOrdered(csvResult::append);
    return csvResult.toString();
  }

  private List<ClaimCsvEntry> getClaimEntries(List<CompositePurchaseOrder> orders, List<Piece> pieces) {
    // Map each PoLine ID to its corresponding Pieces
    var poLineIdToPieces = pieces.stream().collect(groupingBy(Piece::getPoLineId));
    // Map each PoLine ID to its corresponding PoLine
    var poLineIdToPoLine = orders.stream().flatMap(order -> order.getCompositePoLines().stream())
      .collect(Collectors.toMap(CompositePoLine::getId, Function.identity()));
    // Map each Piece ID to its corresponding Title
    var pieceIdToTitle = poLineIdToPieces.entrySet().stream()
      .flatMap(entry -> entry.getValue().stream()
        .map(piece -> Pair.of(piece.getId(), getTitleById(poLineIdToPoLine.get(entry.getKey()), piece)))) // Pair of Piece ID and Title
      .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

    // Key extractor for grouping pieces by: Po Line Number, Display Summary, Chronology, Enumeration, and Title
    Function<Piece, List<String>> keyExtractor = piece -> Arrays.asList(
      poLineIdToPoLine.get(piece.getPoLineId()).getPoLineNumber(),
      piece.getDisplaySummary(),
      piece.getChronology(),
      piece.getEnumeration(),
      pieceIdToTitle.get(piece.getId())
    );

    // Group pieces by the previously defined key
    Map<List<String>, List<Piece>> claimedPieces = pieces.stream()
      .collect(Collectors.groupingBy(keyExtractor));

    // Return a list of ClaimCsvEntry objects, each representing a group of claimed pieces
    // Only the first piece in each group is used to get the Po Line and Title, as they share all necessary attributes
    return claimedPieces.values().stream()
      .map(claims -> new ClaimCsvEntry(
        poLineIdToPoLine.get(claims.get(0).getPoLineId()),
        claims.get(0),
        pieceIdToTitle.get(claims.get(0).getId()),
        claims.size()))
      .sorted(Comparator.comparing(o -> o.compositePoLine().getPoLineNumber()))
      .toList();
  }

  private String getTitleById(CompositePoLine poLine, Piece piece) {
    return Boolean.TRUE.equals(poLine.getIsPackage())
      ? ordersService.getTitleById(piece.getTitleId()).getTitle()
      : poLine.getTitleOrPackage();
  }

}
