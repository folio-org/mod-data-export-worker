package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.OrdersStorageClient;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrdersService {
  private static final int CHUNK_SIZE = 50;
  private final OrdersStorageClient ordersStorageClient;

  public List<PoLine> getPoLinesByQuery(String query) {
    log.debug("OrdersService.getPoLinesByQuery: {}", query);
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
    log.debug("OrdersService.getPoLinesByQuery: returned {} lines", allLines.size());
    return allLines;
  }

  public List<PurchaseOrder> getPurchaseOrdersByIds(List<String> orderIds) {
    log.debug("OrdersService.getPurchaseOrdersByIds: {}", orderIds);
    List<PurchaseOrder> orders = new ArrayList<>();
    Collection<List<String>> idChunks = partitionUsingChunkSize(orderIds);
    for (List<String> idChunk : idChunks) {
      String query = idChunk.stream().collect(Collectors.joining(" OR ", "id==(", ")"));
      orders.addAll(ordersStorageClient.getPurchaseOrdersByQuery(query, 0, Integer.MAX_VALUE).getPurchaseOrders());
    }
    log.debug("OrdersService.getPurchaseOrdersByIds: returned {} orders", orders.size());
    return orders;
  }

  private <T> Collection<List<T>> partitionUsingChunkSize(List<T> inputList) {
    return IntStream.range(0, inputList.size())
      .boxed()
      .collect(Collectors.groupingBy(partition -> partition / CHUNK_SIZE,
        Collectors.mapping(inputList::get, Collectors.toList())))
      .values();
  }
}
