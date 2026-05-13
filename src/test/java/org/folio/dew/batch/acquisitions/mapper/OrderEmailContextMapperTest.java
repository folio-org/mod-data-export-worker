package org.folio.dew.batch.acquisitions.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.folio.dew.batch.acquisitions.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.services.UserService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.templateengine.OrderEmailContext;
import org.folio.dew.domain.dto.templateengine.OrderLineContext;
import org.folio.dew.domain.dto.templateengine.OrderWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderEmailContextMapperTest {

  private static final UUID SHIP_TO_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID BILL_TO_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String SHIP_TO_ADDRESS = "Library Loading Dock, 123 Main St, Springfield IL";
  private static final String BILL_TO_ADDRESS = "Accounts Payable, PO Box 42, Springfield IL";

  @Mock
  private IdentifierTypeService identifierTypeService;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private UserService userService;

  private OrderEmailContextMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mapper = new OrderEmailContextMapper(identifierTypeService, configurationService, userService);
    objectMapper = new ObjectMapper();
    lenient().when(identifierTypeService.getIdentifierTypeName(anyString())).thenReturn("ISBN");
    lenient().when(configurationService.getAddressConfig(any())).thenReturn("");
    lenient().when(userService.getUserName(anyString())).thenReturn("");
  }

  @Test
  void buildContext_mapsOrderFields() throws IOException {
    when(configurationService.getAddressConfig(SHIP_TO_UUID)).thenReturn(SHIP_TO_ADDRESS);
    when(configurationService.getAddressConfig(BILL_TO_UUID)).thenReturn(BILL_TO_ADDRESS);
    when(userService.getUserName("7a626480-284e-5b55-9cf2-db32f93956cf")).thenReturn("John Doe");
    var order = loadOrder("edifact/acquisitions/composite_purchase_order.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders()).hasSize(1);
    var wrapper = ctx.getOrders().get(0);
    assertThat(wrapper.order().getPoNumber()).isEqualTo("10000");
    assertThat(wrapper.order().getOrderDate()).isEqualTo("2021-01-15");
    assertThat(wrapper.order().getOrderType()).isEqualTo("One-Time");
    assertThat(wrapper.order().getCreatedBy()).isEqualTo("John Doe");
    assertThat(wrapper.order().getTotalEstimatedPrice()).isEqualTo("1.8");
    assertThat(wrapper.order().getShipTo()).isEqualTo(SHIP_TO_ADDRESS);
    assertThat(wrapper.order().getBillTo()).isEqualTo(BILL_TO_ADDRESS);
    assertThat(wrapper.order().getNotes()).isEqualTo("First note; Second note");
  }

  @Test
  void buildContext_shipToBillToNotResolved_returnsEmptyString() throws IOException {
    var order = loadOrder("edifact/acquisitions/minimalistic_composite_purchase_order.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders().get(0).order().getShipTo()).isEmpty();
    assertThat(ctx.getOrders().get(0).order().getBillTo()).isEmpty();
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
    assertThat(line.getContributors()).isEqualTo("Moutinho, Luiz");
    assertThat(line.getPublisher()).isEqualTo("Palgrave Macmillan");
    assertThat(line.getPublicationDate()).isEqualTo("2021");
    assertThat(line.getEdition()).isEqualTo("2nd ed.");
    assertThat(line.getProductIdentifier()).isEqualTo("9783319643991");
    assertThat(line.getProductIdentifierType()).isEqualTo("ISBN");
    assertThat(line.getMaterialType()).isEqualTo("1a54b431-2e4f-452d-9cae-9cee66c9a892");
    assertThat(line.getListUnitPrice()).isEqualTo("2.0");
    assertThat(line.getListUnitPriceElectronic()).isEqualTo("3.0");
    assertThat(line.getCurrency()).isEqualTo("USD");
    assertThat(line.getQuantityPhysical()).isEqualTo(1);
    assertThat(line.getQuantityElectronic()).isEqualTo(0);
    assertThat(line.getQuantity()).isEqualTo(1);
    assertThat(line.getEstimatedPrice()).isEqualTo("1.8");
    assertThat(line.getFundCodes()).isEqualTo("USHIST");
    assertThat(line.getVendorRefNumber()).isEqualTo("ORD1000");
    assertThat(line.getInstructions()).isEqualTo("Handle with care");
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
    assertThat(line.getListUnitPrice()).isEmpty();
    assertThat(line.getCurrency()).isEmpty();
    assertThat(line.getQuantity()).isEqualTo(0);
  }

  @Test
  void buildContext_nullVendorDetail_doesNotThrow() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order.json");
    order.getPoLines().get(0).setVendorDetail(null);

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    OrderLineContext line = ctx.getOrders().get(0).orderLines().get(0).orderLine();
    assertThat(line.getVendorRefNumber()).isEmpty();
    assertThat(line.getInstructions()).isEmpty();
  }

  private CompositePurchaseOrder loadOrder(String path) throws IOException {
    return objectMapper.readValue(getMockData(path), CompositePurchaseOrder.class);
  }
}
