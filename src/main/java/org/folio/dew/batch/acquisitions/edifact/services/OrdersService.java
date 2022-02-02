package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.PurchaseOrderClient;
import org.folio.dew.client.PurchaseOrderLineClient;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrdersService {
  private final PurchaseOrderClient purchaseOrderClient;
  private final PurchaseOrderLineClient purchaseOrderLineClient;

  @Cacheable(cacheNames = "orders/composite-orders")
  public CompositePurchaseOrder getCompositePurchaseOrderById(String id) {
    return purchaseOrderClient.getCompositePurchaseOrderById(id);
  }

  @Cacheable(cacheNames = "orders/composite-orders")
  public PurchaseOrderCollection getCompositePurchaseOrderByQuery(String query) {
    return purchaseOrderClient.getCompositePurchaseOrderByQuery(query);
  }

  @Cacheable(cacheNames = "orders/order-lines")
  public CompositePoLine getCompositePoLineById(String id) {
    return purchaseOrderLineClient.getCompositePoLineById(id);
  }

  @Cacheable(cacheNames = "orders/order-lines")
  public PoLineCollection getPoLineByQuery(String query) {
    return purchaseOrderLineClient.getPoLineByQuery(query);
  }

}
