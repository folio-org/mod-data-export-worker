package org.folio.dew.batch.acquisitions.mapper;

import static java.util.stream.Collectors.groupingBy;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.folio.dew.batch.acquisitions.mapper.converter.ClaimCsvFields;
import org.folio.dew.batch.acquisitions.services.OrdersService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.acquisitions.edifact.ClaimCsvEntry;

public class ClaimCsvMapper extends AbstractCsvMapper<ClaimCsvEntry> {

  private final OrdersService ordersService;

  public ClaimCsvMapper(OrdersService ordersService) {
    super(ClaimCsvFields.values());
    this.ordersService = ordersService;
  }

  @Override
  protected List<ClaimCsvEntry> getEntries(List<CompositePurchaseOrder> compPOs, List<Piece> pieces) {
    // Map each PoLine ID to its corresponding Pieces
    var poLineIdToPieces = pieces.stream().collect(groupingBy(Piece::getPoLineId));
    // Map each PoLine ID to its corresponding PoLine
    var poLineIdToPoLine = compPOs.stream().flatMap(order -> order.getPoLines().stream())
      .collect(Collectors.toMap(PoLine::getId, Function.identity()));
    // Map each Piece ID to its corresponding Title
    var pieceIdToTitle = poLineIdToPieces.entrySet().stream()
      .flatMap(entry -> entry.getValue().stream()
        .map(piece -> Pair.of(piece.getId(), getTitleById(poLineIdToPoLine.get(entry.getKey()), piece))))
      .collect(Collectors.toMap(Pair::getLeft, Pair::getRight));

    // Key extractor for grouping pieces by: Po Line Number, Display Summary, Chronology, Enumeration, and Title
    Function<Piece, ClaimCsvEntry> keyExtractor = piece ->
      new ClaimCsvEntry(poLineIdToPoLine.get(piece.getPoLineId()), piece, pieceIdToTitle.get(piece.getId()), 0);

    // Group pieces by the previously defined key (Overridden equals and hashCode methods in ClaimCsvEntry)
    // Only a single piece from each group is used, as they share all necessary attributes
    Map<ClaimCsvEntry, Long> claimedPieces = pieces.stream()
      .collect(Collectors.groupingBy(keyExtractor, Collectors.counting()));

    // Return a list of ClaimCsvEntry objects, each representing a group of claimed pieces
    return claimedPieces.entrySet().stream()
      .map(entry -> entry.getKey().withQuantity(entry.getValue()))
      .sorted(Comparator.comparing(o -> o.poLine().getPoLineNumber()))
      .toList();
  }

  private String getTitleById(PoLine poLine, Piece piece) {
    return Boolean.TRUE.equals(poLine.getIsPackage())
      ? ordersService.getTitleById(piece.getTitleId()).getTitle()
      : poLine.getTitleOrPackage();
  }

}
