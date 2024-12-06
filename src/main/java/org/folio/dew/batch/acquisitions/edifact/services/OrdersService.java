package org.folio.dew.batch.acquisitions.edifact.services;

import static org.folio.dew.utils.QueryUtils.combineCqlExpressions;
import static org.folio.dew.utils.QueryUtils.convertIdsToCqlQuery;

import org.folio.dew.client.OrdersStorageClient;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.folio.dew.utils.QueryUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import one.util.streamex.StreamEx;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrdersService {

  private static final int CHUNK_SIZE = 50;
  private static final String PIECES_BY_REC_STATUS_QUERY = "receivingStatus==%s";


  private final OrdersStorageClient ordersStorageClient;

  public List<PoLine> getPoLinesByQuery(String query) {
    log.debug("getPoLinesByQuery:: Fetching PoLines by query: {}", query);
    var allLines = new ArrayList<PoLine>();
    String lastId = null;
    while (true) {
      String modifiedQuery;
      if (lastId == null)
        modifiedQuery = String.format("%s AND cql.allRecords=1 sortBy id", query);
      else
        modifiedQuery = String.format("%s AND id > %s sortBy id", query, lastId);
      var linesInChunk = ordersStorageClient.getPoLinesByQuery(modifiedQuery, 0, CHUNK_SIZE).getPoLines();
      if (linesInChunk.isEmpty())
        break;
      allLines.addAll(linesInChunk);
      lastId = linesInChunk.get(linesInChunk.size() - 1).getId();
    }
    log.debug("getPoLinesByQuery:: Fetched {} PoLines", allLines.size());
    return allLines;
  }

  public List<PurchaseOrder> getPurchaseOrdersByIds(List<String> orderIds) {
    log.debug("getPurchaseOrdersByIds: Fetching orders: {}", orderIds);
    var orders = StreamEx.ofSubLists(orderIds, CHUNK_SIZE)
      .map(QueryUtils::convertIdsToCqlQuery)
      .map(query -> ordersStorageClient.getPurchaseOrdersByQuery(query, 0, Integer.MAX_VALUE))
      .flatMap(collection -> collection.getPurchaseOrders().stream())
      .toList();
    log.debug("getPurchaseOrdersByIds:: Fetched {} orders", orders.size());
    return orders;
  }

  public List<Piece> getPiecesByIdsAndReceivingStatus(List<String> pieceIds, Piece.ReceivingStatusEnum receivingStatus) {
    log.debug("getPiecesByIdsAndReceivingStatus:: Fetching pieces: {} by status: {}", pieceIds, receivingStatus);
    var receivingStatusQuery = PIECES_BY_REC_STATUS_QUERY.formatted(receivingStatus.getValue());
    var pieces = StreamEx.ofSubLists(pieceIds, CHUNK_SIZE)
      .map(ids -> combineCqlExpressions("and", convertIdsToCqlQuery(ids), receivingStatusQuery))
      .map(query -> ordersStorageClient.getPiecesByQuery(query, 0, Integer.MAX_VALUE))
      .flatMap(collection -> collection.getPieces().stream())
      .toList();
    log.debug("getPiecesByIdsAndReceivingStatus:: Fetched {} pieces", pieces.size());
    return pieces;
  }

}
