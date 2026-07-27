package org.folio.dew.batch.acquisitions.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dew.batch.acquisitions.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.services.ContributorNameTypeService;
import org.folio.dew.batch.acquisitions.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.batch.acquisitions.services.UserService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.folio.dew.domain.dto.acquisitions.edifact.OrganizationAddress;
import org.folio.dew.domain.dto.acquisitions.edifact.TenantAddress;
import org.folio.dew.domain.dto.templateengine.context.OrderEmailContext;
import org.folio.dew.domain.dto.templateengine.context.OrderLineContext;
import org.folio.dew.domain.dto.templateengine.context.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.dew.utils.TestUtils.getMockData;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEmailContextMapperTest {

  private static final UUID SHIP_TO_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID BILL_TO_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final String SHIP_TO_ADDRESS = "Library Loading Dock, 123 Main St, Springfield IL";
  private static final String BILL_TO_ADDRESS = "Accounts Payable, PO Box 42, Springfield IL";
  private static final String VENDOR_UUID = "50fb6ae0-cdf1-11e8-a8d5-f2801f1b9fd1";

  @Mock
  private IdentifierTypeService identifierTypeService;
  @Mock
  private ContributorNameTypeService contributorNameTypeService;
  @Mock
  private ConfigurationService configurationService;
  @Mock
  private UserService userService;
  @Mock
  private OrganizationsService organizationsService;

  private OrderEmailContextMapper mapper;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    mapper = new OrderEmailContextMapper(identifierTypeService, contributorNameTypeService, configurationService, userService, organizationsService);
    objectMapper = new ObjectMapper();
    lenient().when(identifierTypeService.getIdentifierTypeName(anyString())).thenReturn("ISBN");
    lenient().when(contributorNameTypeService.getContributorNameTypeName(anyString())).thenReturn("Personal name");
    lenient().when(configurationService.getTenantAddress(any())).thenReturn(null);
    lenient().when(userService.getUserContext(anyString())).thenReturn(UserContext.builder().build());
    lenient().when(organizationsService.getOrganizationById(anyString())).thenReturn(new Organization());
  }

  @Test
  void buildContext_mapsOrderFields() throws IOException {
    when(configurationService.getTenantAddress(SHIP_TO_UUID)).thenReturn(tenantAddress(SHIP_TO_UUID, SHIP_TO_ADDRESS));
    when(configurationService.getTenantAddress(BILL_TO_UUID)).thenReturn(tenantAddress(BILL_TO_UUID, BILL_TO_ADDRESS));
    var johnDoe = UserContext.builder().id("7a626480-284e-5b55-9cf2-db32f93956cf").firstName("John").lastName("Doe").fullName("John Doe").build();
    when(userService.getUserContext("7a626480-284e-5b55-9cf2-db32f93956cf")).thenReturn(johnDoe);
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders()).hasSize(1);
    var wrapper = ctx.getOrders().get(0);
    assertThat(wrapper.order().getPoNumber()).isEqualTo("10000");
    assertThat(wrapper.order().getOrderType()).isEqualTo("One-Time");
    assertThat(wrapper.order().getMetadata().getCreatedByUser().getId()).isEqualTo("7a626480-284e-5b55-9cf2-db32f93956cf");
    assertThat(wrapper.order().getMetadata().getCreatedByUser()).isEqualTo(johnDoe);
    assertThat(wrapper.order().getMetadata().getCreatedByUser().getFullName()).isEqualTo("John Doe");
    assertThat(wrapper.order().getShipTo().getId()).isEqualTo(SHIP_TO_UUID.toString());
    assertThat(wrapper.order().getShipTo().getAddress()).isEqualTo(SHIP_TO_ADDRESS);
    assertThat(wrapper.order().getBillTo().getId()).isEqualTo(BILL_TO_UUID.toString());
    assertThat(wrapper.order().getBillTo().getAddress()).isEqualTo(BILL_TO_ADDRESS);
  }

  @Test
  void buildContext_shipToBillToNewlines_keptVerbatim() throws IOException {
    when(configurationService.getTenantAddress(SHIP_TO_UUID))
      .thenReturn(tenantAddress(SHIP_TO_UUID, "SLUB Dresden\nZellescher Weg 18\n01069 Dresden"));
    when(configurationService.getTenantAddress(BILL_TO_UUID))
      .thenReturn(tenantAddress(BILL_TO_UUID, "Accounts Payable\nPO Box 42\nSpringfield IL"));
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders().get(0).order().getShipTo().getAddress())
      .isEqualTo("SLUB Dresden\nZellescher Weg 18\n01069 Dresden");
    assertThat(ctx.getOrders().get(0).order().getBillTo().getAddress())
      .isEqualTo("Accounts Payable\nPO Box 42\nSpringfield IL");
  }

  @Test
  void buildContext_shipToBillToNotResolved_returnsEmptyString() throws IOException {
    var order = loadOrder("edifact/acquisitions/minimalistic_composite_purchase_order.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders().get(0).order().getShipTo().getAddress()).isEmpty();
    assertThat(ctx.getOrders().get(0).order().getBillTo().getAddress()).isEmpty();
  }

  @Test
  void buildContext_mapsOrderLineFields() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrders()).hasSize(1);
    var lines = ctx.getOrders().get(0).orderLines();
    assertThat(lines).hasSize(1);

    OrderLineContext line = lines.get(0).orderLine();
    assertThat(line.getPoLineNumber()).isEqualTo("10000-1");
    assertThat(line.getTitleOrPackage()).isEqualTo("Futures, biometrics and neuroscience research Luiz Moutinho, Mladen Sokele, editors");
    assertThat(line.getPublisher()).isEqualTo("Palgrave Macmillan");
    assertThat(line.getPublicationDate()).isEqualTo("2021");
    assertThat(line.getEdition()).isEqualTo("2nd ed.");
    assertThat(line.getRush()).isFalse();
    assertThat(line.getContributors()).hasSize(1);
    var contributor = line.getContributors().get(0);
    assertThat(contributor.getContributor()).isEqualTo("Moutinho, Luiz");
    assertThat(contributor.getContributorNameType().getId()).isEqualTo("2b94c631-fca9-4892-a730-03ee529ffe2a");
    assertThat(contributor.getContributorNameType().getName()).isEqualTo("Personal name");
    assertThat(line.getDetails().getProductIds()).hasSize(1);
    var productId = line.getDetails().getProductIds().get(0);
    assertThat(productId.getProductId()).isEqualTo("9783319643991");
    assertThat(productId.getQualifier()).isEqualTo("(paperback)");
    assertThat(productId.getProductIdType().getId()).isEqualTo("8261054f-be78-422d-bd51-4ed9f33c3422");
    assertThat(productId.getProductIdType().getName()).isEqualTo("ISBN");
    assertThat(line.getCost().getListUnitPrice()).isEqualTo("2.0");
    assertThat(line.getCost().getCurrency()).isEqualTo("USD");
    assertThat(line.getCost().getPoLineEstimatedPrice()).isEqualTo("1.8");
    assertThat(line.getFundDistribution()).hasSize(1);
    var fund = line.getFundDistribution().get(0);
    assertThat(fund.getCode()).isEqualTo("USHIST");
    assertThat(line.getVendorDetail().getInstructions()).isEqualTo("Handle with care");
  }

  @Test
  void buildContext_multipleOrders_producesOneWrapperPerOrder() throws IOException {
    var order1 = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");
    var order2 = loadOrder("edifact/acquisitions/minimalistic_composite_purchase_order.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order1, order2));

    assertThat(ctx.getOrders()).hasSize(2);
  }

  @Test
  void buildContext_emptyOrders_returnsEmptyList() {
    OrderEmailContext ctx = mapper.buildContext(List.of());

    assertThat(ctx.getOrders()).isEmpty();
    assertThat(ctx.getOrganization().getName()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getAddressLine1()).isEmpty();
  }

  @Test
  void buildContext_setsCreatedAt() {
    OrderEmailContext ctx = mapper.buildContext(List.of());

    assertThat(ctx.getCreatedAt()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z");
  }

  @Test
  void buildContext_mapsOrganizationFields() throws IOException {
    var org = new Organization();
    org.setName("Acme Books");
    org.setCode("ACME");
    org.setAddresses(List.of(
      buildAddress("321 Other St", "Otherville", "99999", "USA", false),
      buildAddress("100 Main St", "Springfield", "12345", "USA", true)));
    when(organizationsService.getOrganizationById(VENDOR_UUID)).thenReturn(org);
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    var organization = ctx.getOrganization();
    assertThat(organization.getName()).isEqualTo("Acme Books");
    assertThat(organization.getPrimaryAddress().getAddressLine1()).isEqualTo("100 Main St");
    assertThat(organization.getPrimaryAddress().getCity()).isEqualTo("Springfield");
    assertThat(organization.getPrimaryAddress().getZipCode()).isEqualTo("12345");
    assertThat(organization.getPrimaryAddress().getCountry()).isEqualTo("USA");
  }

  @Test
  void buildContext_noPrimaryFlagged_returnsEmpty() throws IOException {
    var org = new Organization();
    org.setAddresses(List.of(buildAddress("First St", "FirstCity", "11111", "USA", null)));
    when(organizationsService.getOrganizationById(VENDOR_UUID)).thenReturn(org);
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrganization().getPrimaryAddress().getAddressLine1()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getCity()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getZipCode()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getCountry()).isEmpty();
  }

  @Test
  void buildContext_organizationWithEmptyLists_returnsEmptyStrings() throws IOException {
    var org = new Organization();
    org.setName("Acme Books");
    when(organizationsService.getOrganizationById(VENDOR_UUID)).thenReturn(org);
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrganization().getName()).isEqualTo("Acme Books");
    assertThat(ctx.getOrganization().getPrimaryAddress().getAddressLine1()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getCity()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getZipCode()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getCountry()).isEmpty();
  }

  private OrganizationAddress buildAddress(String line1, String city, String zip, String country, Boolean isPrimary) {
    var address = new OrganizationAddress();
    address.setAddressLine1(line1);
    address.setCity(city);
    address.setZipCode(zip);
    address.setCountry(country);
    address.setIsPrimary(isPrimary);
    return address;
  }

  @Test
  void buildContext_nullCost_doesNotThrow() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");
    order.getPoLines().get(0).setCost(null);

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    OrderLineContext line = ctx.getOrders().get(0).orderLines().get(0).orderLine();
    assertThat(line.getCost().getListUnitPrice()).isEmpty();
    assertThat(line.getCost().getCurrency()).isEmpty();
  }

  @Test
  void buildContext_instructionsNewlines_keptVerbatim() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");
    order.getPoLines().get(0).getVendorDetail().setInstructions("Line 1\nLine 2");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    OrderLineContext line = ctx.getOrders().get(0).orderLines().get(0).orderLine();
    assertThat(line.getVendorDetail().getInstructions()).isEqualTo("Line 1\nLine 2");
  }

  @Test
  void buildContext_nullVendorDetail_doesNotThrow() throws IOException {
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");
    order.getPoLines().get(0).setVendorDetail(null);

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    OrderLineContext line = ctx.getOrders().get(0).orderLines().get(0).orderLine();
    assertThat(line.getVendorDetail().getInstructions()).isEmpty();
  }

  @Test
  void buildContext_organizationLookupThrows_returnsEmptyOrganization() throws IOException {
    when(organizationsService.getOrganizationById(VENDOR_UUID)).thenThrow(new RuntimeException("boom"));
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    assertThat(ctx.getOrganization().getName()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getAddressLine1()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getCity()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getZipCode()).isEmpty();
    assertThat(ctx.getOrganization().getPrimaryAddress().getCountry()).isEmpty();
  }

  @Test
  void buildContext_identifierTypeLookupThrows_returnsEmptyName() throws IOException {
    when(identifierTypeService.getIdentifierTypeName(anyString())).thenThrow(new RuntimeException("boom"));
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    var productIdType = ctx.getOrders().get(0).orderLines().get(0).orderLine()
      .getDetails().getProductIds().get(0).getProductIdType();
    assertThat(productIdType.getName()).isEmpty();
    assertThat(productIdType.getId()).isEqualTo("8261054f-be78-422d-bd51-4ed9f33c3422");
  }

  @Test
  void buildContext_contributorNameTypeLookupThrows_returnsEmptyName() throws IOException {
    when(contributorNameTypeService.getContributorNameTypeName(anyString())).thenThrow(new RuntimeException("boom"));
    var order = loadOrder("edifact/acquisitions/composite_purchase_order_email_context.json");

    OrderEmailContext ctx = mapper.buildContext(List.of(order));

    var contributorNameType = ctx.getOrders().get(0).orderLines().get(0).orderLine()
      .getContributors().get(0).getContributorNameType();
    assertThat(contributorNameType.getName()).isEmpty();
    assertThat(contributorNameType.getId()).isEqualTo("2b94c631-fca9-4892-a730-03ee529ffe2a");
  }

  private CompositePurchaseOrder loadOrder(String path) throws IOException {
    return objectMapper.readValue(getMockData(path), CompositePurchaseOrder.class);
  }

  private TenantAddress tenantAddress(UUID id, String address) {
    var tenantAddress = new TenantAddress();
    tenantAddress.setId(id);
    tenantAddress.setAddress(address);
    return tenantAddress;
  }
}
