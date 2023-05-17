package org.folio.dew.batch.acquisitions.edifact;

import org.folio.dew.batch.acquisitions.edifact.services.OrdersService;
import org.folio.dew.client.OrdersStorageClient;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.PoLineCollection;
import org.folio.dew.domain.dto.PurchaseOrder;
import org.folio.dew.domain.dto.PurchaseOrderCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
  @InjectMocks
  private OrdersService orderService;
  @Mock
  OrdersStorageClient ordersStorageClient;

  @Test
  void getPoLinesByQueryTest() {
    PoLine poLine = new PoLine()
      .id("one");
    String query = "test";
    String modifiedQuery1 = "test AND cql.allRecords=1 sortBy id";
    String modifiedQuery2 = "test AND id > one sortBy id";
    PoLineCollection poLineCollection1 = new PoLineCollection()
      .poLines(List.of(poLine))
      .totalRecords(1);
    PoLineCollection poLineCollection2 = new PoLineCollection()
      .poLines(List.of())
      .totalRecords(0);
    doReturn(poLineCollection1)
      .when(ordersStorageClient).getPoLinesByQuery(modifiedQuery1, 0, 50);
    doReturn(poLineCollection2)
      .when(ordersStorageClient).getPoLinesByQuery(modifiedQuery2, 0, 50);

    List<PoLine> lines = orderService.getPoLinesByQuery(query);

    assertEquals(lines, List.of(poLine));
    verify(ordersStorageClient).getPoLinesByQuery(modifiedQuery1, 0, 50);
    verify(ordersStorageClient).getPoLinesByQuery(modifiedQuery2, 0, 50);
  }

  @Test
  void getPurchaseOrdersByIdsTest() {
    List<String> allOrderIds = new ArrayList<>();
    List<PurchaseOrder> allOrders = new ArrayList<>();
    List<PurchaseOrder> orderList1 = new ArrayList<>();
    List<PurchaseOrder> orderList2 = new ArrayList<>();
    StringBuilder query1Builder = new StringBuilder();
    query1Builder.append("id==(");
    StringBuilder query2Builder = new StringBuilder();
    query2Builder.append("id==(");
    for (int i=0; i<60; i++) {
      String id = Integer.toString(i);
      allOrderIds.add(id);
      PurchaseOrder order = new PurchaseOrder().id(id);
      allOrders.add(order);
      if (i < 50) {
        orderList1.add(order);
        query1Builder.append(id);
        if (i != 49)
          query1Builder.append(" OR ");
      } else {
        orderList2.add(order);
        query2Builder.append(id);
        if (i != 59)
          query2Builder.append(" OR ");
      }
    }
    query1Builder.append(")");
    query2Builder.append(")");
    String query1 = query1Builder.toString();
    String query2 = query2Builder.toString();
    PurchaseOrderCollection orderCollection1 = new PurchaseOrderCollection()
      .purchaseOrders(orderList1)
      .totalRecords(orderList1.size());
    PurchaseOrderCollection orderCollection2 = new PurchaseOrderCollection()
      .purchaseOrders(orderList2)
      .totalRecords(orderList2.size());
    doReturn(orderCollection1)
      .when(ordersStorageClient).getPurchaseOrdersByQuery(query1, 0, Integer.MAX_VALUE);
    doReturn(orderCollection2)
      .when(ordersStorageClient).getPurchaseOrdersByQuery(query2, 0, Integer.MAX_VALUE);

    List<PurchaseOrder> orders = orderService.getPurchaseOrdersByIds(allOrderIds);

    assertEquals(orders, allOrders);
    verify(ordersStorageClient).getPurchaseOrdersByQuery(query1, 0, Integer.MAX_VALUE);
    verify(ordersStorageClient).getPurchaseOrdersByQuery(query2, 0, Integer.MAX_VALUE);
  }
}
