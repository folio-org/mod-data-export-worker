package org.folio.dew.batch.acquisitions.edifact.services;

import org.folio.dew.client.PurchaseOrderClient;
import org.folio.dew.client.PurchaseOrderLineClient;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class OrdersService {
  private final PurchaseOrderClient purchaseOrderClient;
  private final PurchaseOrderLineClient purchaseOrderLineClient;

  public CompositePurchaseOrder getCompositePurchaseOrderById(String id) {
    return purchaseOrderClient.getCompositePurchaseOrderById(id);
  }

  public PurchaseOrderCollection getCompositePurchaseOrderByQuery(String query) {
    return purchaseOrderClient.getCompositePurchaseOrderByQuery(query);
  }

  public CompositePoLine getCompositePoLineById(String id) {
    return purchaseOrderLineClient.getCompositePoLineById(id);
  }

  public PoLineCollection getPoLineByQuery(String query) {
    return purchaseOrderLineClient.getPoLineByQuery(query);
  }

}
