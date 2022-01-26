package org.folio.dew.batch.acquisitions.edifact.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.dew.client.PurchaseOrderClient;
import org.folio.dew.client.PurchaseOrderLineClient;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrdersService {
  private final PurchaseOrderClient purchaseOrderClient;
  private final PurchaseOrderLineClient purchaseOrderLineClient;

  @Cacheable(cacheNames = "orders/purchase-orders")
  public CompositePurchaseOrder getCompositePurchaseOrderById(String id) {
    return purchaseOrderClient.getCompositePurchaseOrderById(id);
  }

  @Cacheable(cacheNames = "orders/purchase-orders")
  public CompositePurchaseOrder getCompositePurchaseOrderByQuery(String query) {
    return purchaseOrderClient.getCompositePurchaseOrderById(query);
  }

  @Cacheable(cacheNames = "orders/order-lines")
  public CompositePoLine getCompositePoLineById(String id) {
    return purchaseOrderLineClient.getCompositePurchaseCompositePoLineById(id);
  }

}
