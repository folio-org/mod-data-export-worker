package org.folio.dew.batch.acquisitions.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.utils.TestUtils.getMockData;

import java.io.IOException;
import java.util.List;

import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.templateengine.OrderEmailContext;
import org.folio.dew.domain.dto.templateengine.OrderLineContext;
import org.folio.dew.domain.dto.templateengine.OrderWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class OrderEmailContextMapperTest {

  private OrderEmailContextMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mapper = new OrderEmailContextMapper();
    objectMapper = new ObjectMapper();
  }

  @Test
  void buildContext_mapsOrderFields() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders()).hasSize(1);
    var wrapper = ctx.getOrders().get(0);
    assertThat(wrapper.order().getPoNumber()).isEqualTo("10000");
    assertThat(wrapper.order().getOrderType()).isEqualTo("One-Time");
  }

  @Test
  void buildContext_mapsOrderLineFields() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders()).hasSize(1);
    var lines = ctx.getOrders().get(0).orderLines();
    assertThat(lines).hasSize(1);

    OrderLineContext line = lines.get(0).orderLine();
    assertThat(line.getPoLineNumber()).isEqualTo("10000-1");
    assertThat(line.getTitle()).isEqualTo("Futures, biometrics and neuroscience research Luiz Moutinho, Mladen Sokele, editors");
    assertThat(line.getPublisher()).isEqualTo("Palgrave Macmillan");
    assertThat(line.getListUnitPrice()).isEqualTo("2.0");
    assertThat(line.getCurrency()).isEqualTo("USD");
    assertThat(line.getQuantityPhysical()).isEqualTo(1);
    assertThat(line.getQuantityElectronic()).isEqualTo(0);
    assertThat(line.getQuantity()).isEqualTo(1);
    assertThat(line.getEstimatedPrice()).isEqualTo("1.8");
    assertThat(line.getFundCodes()).isEqualTo("USHIST");
    assertThat(line.getVendorRefNumber()).isEqualTo("ORD1000");
  }

  @Test
  void buildContext_multipleOrders_producesOneWrapperPerOrder() throws IOException {
    var order1 = loadOrder("edifact/acquisitions/composite_purchase_order.json");
    var order2 = loadOrder("edifact/acquisitions/minimalistic_composite_purchase_order.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order1, order2));

    assertThat(ctx.getOrders()).hasSize(2);
  }

  @Test
  void buildContext_emptyOrders_returnsEmptyList() {
    OrderEmailContext ctx = mapper.buildContext(List.of());

    assertThat(ctx.getOrders()).isEmpty();
  }

  @Test
  void buildContext_nullCost_doesNotThrow() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order.json");
    order.getPoLines().get(0).setCost(null);

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    OrderLineContext line = ctx.getOrders().get(0).orderLines().get(0).orderLine();
    assertThat(line.getListUnitPrice()).isNull();
    assertThat(line.getCurrency()).isNull();
    assertThat(line.getQuantity()).isEqualTo(0);
  }

  @Test
  void buildContext_nullVendorDetail_doesNotThrow() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order.json");
    order.getPoLines().get(0).setVendorDetail(null);

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    OrderLineContext line = ctx.getOrders().get(0).orderLines().get(0).orderLine();
    assertThat(line.getVendorRefNumber()).isNull();
    assertThat(line.getInstructions()).isNull();
  }

  private CompositePurchaseOrder loadOrder(String path) throws IOException {
    return objectMapper.readValue(getMockData(path), CompositePurchaseOrder.class);
  }
}
