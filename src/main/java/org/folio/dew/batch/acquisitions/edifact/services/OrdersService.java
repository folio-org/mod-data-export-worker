package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.OrdersClient;
import org.folio.dew.client.OrdersStorageClient;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrdersService {
  private final OrdersClient ordersClient;
  private final OrdersStorageClient ordersStorageClient;

  public CompositePurchaseOrder getCompositePurchaseOrderById(String id) {
    return ordersClient.getCompositePurchaseOrderById(id);
  }

  public PurchaseOrderCollection getCompositePurchaseOrderByQuery(String query, int limit) {
    return ordersStorageClient.getCompositePurchaseOrderByQuery(query, limit);
  }

}
