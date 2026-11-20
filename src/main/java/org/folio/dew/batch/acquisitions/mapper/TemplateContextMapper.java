package org.folio.dew.batch.acquisitions.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.acquisitions.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.services.ContributorNameTypeService;
import org.folio.dew.batch.acquisitions.services.CustomFieldsService;
import org.folio.dew.batch.acquisitions.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.services.OrdersService;
import org.folio.dew.batch.acquisitions.services.OrganizationsService;
import org.folio.dew.batch.acquisitions.services.UserService;
import org.folio.dew.domain.dto.*;
import org.folio.dew.domain.dto.acquisitions.edifact.Organization;
import org.folio.dew.domain.dto.acquisitions.edifact.OrganizationAddress;
import org.folio.dew.domain.dto.templateengine.context.BaseTemplateContext;
import org.folio.dew.domain.dto.templateengine.context.ClaimOrderLineContext;
import org.folio.dew.domain.dto.templateengine.context.ClaimingTemplateContext;
import org.folio.dew.domain.dto.templateengine.context.ContributorContext;
import org.folio.dew.domain.dto.templateengine.context.CostContext;
import org.folio.dew.domain.dto.templateengine.context.DetailsContext;
import org.folio.dew.domain.dto.templateengine.context.FundDistributionContext;
import org.folio.dew.domain.dto.templateengine.context.OrderContext;
import org.folio.dew.domain.dto.templateengine.context.OrderingTemplateContext;
import org.folio.dew.domain.dto.templateengine.context.OrderLineContext;
import org.folio.dew.domain.dto.templateengine.context.OrderLineWrapper;
import org.folio.dew.domain.dto.templateengine.context.OrderMetadataContext;
import org.folio.dew.domain.dto.templateengine.context.OrderWrapper;
import org.folio.dew.domain.dto.templateengine.context.OrganizationAddressContext;
import org.folio.dew.domain.dto.templateengine.context.OrganizationContext;
import org.folio.dew.domain.dto.templateengine.context.PieceContext;
import org.folio.dew.domain.dto.templateengine.context.ProductIdContext;
import org.folio.dew.domain.dto.templateengine.context.TenantAddressContext;
import org.folio.dew.domain.dto.templateengine.context.TypeContext;
import org.folio.dew.domain.dto.templateengine.context.VendorDetailContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.groupingBy;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getFormattedDate;

@Component
@RequiredArgsConstructor
@Log4j2
public class TemplateContextMapper {

  private static final DateTimeFormatter CREATED_AT_FORMATTER = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
  private static final String ENTITY_TYPE_PURCHASE_ORDER = "purchase_order";
  private static final String ENTITY_TYPE_PO_LINE = "po_line";
  private static final Comparator<Piece> PIECE_ORDER = Comparator
    .comparing(Piece::getReceiptDate, nullsLast(naturalOrder()))
    .thenComparing(Piece::getDisplaySummary, nullsLast(naturalOrder()));

  private final IdentifierTypeService identifierTypeService;
  private final ContributorNameTypeService contributorNameTypeService;
  private final ConfigurationService configurationService;
  private final UserService userService;
  private final OrganizationsService organizationsService;
  private final CustomFieldsService customFieldsService;
  private final OrdersService ordersService;

  public OrderingTemplateContext buildContext(List<CompositePurchaseOrder> orders) {
    return fillContext(OrderingTemplateContext.builder(), orders,
        line -> fillOrderLine(OrderLineContext.builder(), line).build())
      .build();
  }

  public ClaimingTemplateContext buildClaimContext(List<CompositePurchaseOrder> orders, List<Piece> pieces) {
    var piecesByPoLineId = pieces.stream().collect(groupingBy(Piece::getPoLineId));
    return fillContext(ClaimingTemplateContext.builder(), orders,
        line -> fillOrderLine(ClaimOrderLineContext.builder(), line)
          .pieces(mapPieces(line, piecesByPoLineId.getOrDefault(line.getId(), List.of())))
          .build())
      .build();
  }

  private <B extends BaseTemplateContext.BaseTemplateContextBuilder<?, ?>> B fillContext(B builder,
      List<CompositePurchaseOrder> orders, Function<PoLine, OrderLineContext> lineMapper) {
    builder.createdAt(formatCreatedAt(Instant.now()))
      .organization(mapOrganization(orders))
      .orders(mapOrders(orders, lineMapper));
    return builder;
  }

  private List<OrderWrapper> mapOrders(List<CompositePurchaseOrder> orders, Function<PoLine, OrderLineContext> lineMapper) {
    return orders.stream()
      .map(order -> new OrderWrapper(
        mapOrder(order),
        order.getPoLines().stream()
          .map(line -> new OrderLineWrapper(lineMapper.apply(line)))
          .toList()))
      .toList();
  }

  static String formatCreatedAt(Instant instant) {
    return CREATED_AT_FORMATTER.format(instant);
  }

  private OrganizationContext mapOrganization(List<CompositePurchaseOrder> orders) {
    return orders.stream()
      .map(CompositePurchaseOrder::getVendor)
      .filter(Objects::nonNull)
      .findFirst()
      .map(this::resolveOrganization)
      .map(org -> OrganizationContext.builder()
        .name(StringUtils.defaultString(org.getName()))
        .primaryAddress(pickPrimaryAddress(org.getAddresses()))
        .build())
      .orElseGet(this::emptyOrganizationContext);
  }

  private Organization resolveOrganization(UUID vendorId) {
    try {
      return organizationsService.getOrganizationById(vendorId.toString());
    } catch (Exception e) {
      log.warn("resolveOrganization:: Cannot resolve vendor organization '{}' for email context", vendorId, e);
      return null;
    }
  }

  private OrganizationContext emptyOrganizationContext() {
    return OrganizationContext.builder()
      .name("")
      .primaryAddress(emptyOrganizationAddressContext())
      .build();
  }

  private OrganizationAddressContext emptyOrganizationAddressContext() {
    return OrganizationAddressContext.builder()
      .addressLine1("")
      .city("")
      .zipCode("")
      .country("")
      .build();
  }

  private OrganizationAddressContext pickPrimaryAddress(List<OrganizationAddress> addresses) {
    return pickPrimary(addresses, addr -> Boolean.TRUE.equals(addr.getIsPrimary()))
      .map(addr -> OrganizationAddressContext.builder()
        .addressLine1(StringUtils.defaultString(addr.getAddressLine1()))
        .city(StringUtils.defaultString(addr.getCity()))
        .zipCode(StringUtils.defaultString(addr.getZipCode()))
        .country(StringUtils.defaultString(addr.getCountry()))
        .build())
      .orElseGet(this::emptyOrganizationAddressContext);
  }

  private <T> Optional<T> pickPrimary(List<T> items, Predicate<T> isPrimary) {
    if (CollectionUtils.isEmpty(items)) {
      return Optional.empty();
    }
    return items.stream()
      .filter(isPrimary)
      .findFirst();
  }

  private OrderContext mapOrder(CompositePurchaseOrder order) {
    return OrderContext.builder()
      .poNumber(StringUtils.defaultString(order.getPoNumber()))
      .orderType(Optional.ofNullable(order.getOrderType()).map(CompositePurchaseOrder.OrderTypeEnum::getValue).orElse(""))
      .metadata(mapOrderMetadata(order.getMetadata()))
      .shipTo(mapTenantAddress(order.getShipTo()))
      .billTo(mapTenantAddress(order.getBillTo()))
      .customFields(customFieldsService.resolve(order.getCustomFields(), ENTITY_TYPE_PURCHASE_ORDER))
      .build();
  }

  private String toUuidString(UUID id) {
    return Optional.ofNullable(id).map(UUID::toString).orElse("");
  }

  private TenantAddressContext mapTenantAddress(UUID addressId) {
    var tenantAddress = configurationService.getTenantAddress(addressId);
    if (tenantAddress == null) {
      return TenantAddressContext.builder().id("").address("").build();
    }
    return TenantAddressContext.builder()
      .id(toUuidString(tenantAddress.getId()))
      .address(StringUtils.defaultString(tenantAddress.getAddress()))
      .build();
  }

  private OrderMetadataContext mapOrderMetadata(Metadata metadata) {
    var createdByUserId = Optional.ofNullable(metadata)
      .map(Metadata::getCreatedByUserId)
      .map(Object::toString)
      .orElse("");
    return OrderMetadataContext.builder()
      .createdByUser(userService.getUserContext(createdByUserId))
      .build();
  }

  private <B extends OrderLineContext.OrderLineContextBuilder<?, ?>> B fillOrderLine(B builder, PoLine line) {
    builder.poLineNumber(StringUtils.defaultString(line.getPoLineNumber()))
      .titleOrPackage(StringUtils.defaultString(line.getTitleOrPackage()))
      .publisher(StringUtils.defaultString(line.getPublisher()))
      .publicationDate(StringUtils.defaultString(line.getPublicationDate()))
      .edition(StringUtils.defaultString(line.getEdition()))
      .rush(Optional.ofNullable(line.getRush()).orElse(false))
      .contributors(mapList(line.getContributors(), this::mapContributor))
      .details(mapDetails(line.getDetails()))
      .cost(mapCost(line.getCost()))
      .fundDistribution(mapList(line.getFundDistribution(), this::mapFundDistribution))
      .vendorDetail(mapVendorDetail(line.getVendorDetail()))
      .customFields(customFieldsService.resolve(line.getCustomFields(), ENTITY_TYPE_PO_LINE));
    return builder;
  }

  private List<PieceContext> mapPieces(PoLine line, List<Piece> pieces) {
    return pieces.stream()
      .sorted(PIECE_ORDER)
      .map(piece -> mapPiece(line, piece))
      .toList();
  }

  private PieceContext mapPiece(PoLine line, Piece piece) {
    return PieceContext.builder()
      .title(StringUtils.defaultString(ordersService.getPieceTitle(line, piece)))
      .displaySummary(StringUtils.defaultString(piece.getDisplaySummary()))
      .enumeration(StringUtils.defaultString(piece.getEnumeration()))
      .chronology(StringUtils.defaultString(piece.getChronology()))
      .format(Optional.ofNullable(piece.getFormat()).map(Piece.FormatEnum::getValue).orElse(""))
      .supplement(Optional.ofNullable(piece.getSupplement()).orElse(false))
      .receiptDate(getFormattedDate(piece.getReceiptDate()))
      .externalNote(StringUtils.defaultString(piece.getExternalNote()))
      .build();
  }

  private <S, T> List<T> mapList(List<S> source, Function<S, T> mapper) {
    if (CollectionUtils.isEmpty(source)) {
      return List.of();
    }
    return source.stream()
      .filter(Objects::nonNull)
      .map(mapper)
      .toList();
  }

  private FundDistributionContext mapFundDistribution(FundDistribution fundDistribution) {
    return FundDistributionContext.builder()
      .code(StringUtils.defaultString(fundDistribution.getCode()))
      .build();
  }

  private CostContext mapCost(Cost cost) {
    var quantityPhysical = Optional.ofNullable(cost).map(Cost::getQuantityPhysical).orElse(0);
    var quantityElectronic = Optional.ofNullable(cost).map(Cost::getQuantityElectronic).orElse(0);
    return CostContext.builder()
      .listUnitPrice(formatDecimal(Optional.ofNullable(cost).map(Cost::getListUnitPrice).orElse(null)))
      .listUnitPriceElectronic(formatDecimal(Optional.ofNullable(cost).map(Cost::getListUnitPriceElectronic).orElse(null)))
      .quantityPhysical(quantityPhysical)
      .quantityElectronic(quantityElectronic)
      .poLineEstimatedPrice(formatDecimal(Optional.ofNullable(cost).map(Cost::getPoLineEstimatedPrice).orElse(null)))
      .currency(Optional.ofNullable(cost).map(Cost::getCurrency).orElse(""))
      .build();
  }

  private String formatDecimal(BigDecimal value) {
    return value != null ? value.toPlainString() : "";
  }

  private VendorDetailContext mapVendorDetail(VendorDetail vendorDetail) {
    return VendorDetailContext.builder()
      .instructions(Optional.ofNullable(vendorDetail).map(VendorDetail::getInstructions).orElse(""))
      .build();
  }

  private ContributorContext mapContributor(Contributor contributor) {
    var contributorNameTypeId = StringUtils.defaultString(contributor.getContributorNameTypeId());
    return ContributorContext.builder()
      .contributor(StringUtils.defaultString(contributor.getContributor()))
      .contributorNameType(TypeContext.builder()
        .id(contributorNameTypeId)
        .name(StringUtils.isBlank(contributorNameTypeId) ? "" : resolveContributorNameTypeName(contributorNameTypeId))
        .build())
      .build();
  }

  private DetailsContext mapDetails(Details details) {
    return DetailsContext.builder()
      .productIds(mapList(details == null ? null : details.getProductIds(), this::mapProductId))
      .build();
  }

  private ProductIdContext mapProductId(ProductIdentifier productId) {
    var productIdTypeId = StringUtils.defaultString(productId.getProductIdType());
    return ProductIdContext.builder()
      .productId(StringUtils.defaultString(productId.getProductId()))
      .qualifier(StringUtils.defaultString(productId.getQualifier()))
      .productIdType(TypeContext.builder()
        .id(productIdTypeId)
        .name(StringUtils.isBlank(productIdTypeId) ? "" : resolveIdentifierTypeName(productIdTypeId))
        .build())
      .build();
  }

  private String resolveIdentifierTypeName(String productIdTypeId) {
    try {
      return StringUtils.defaultString(identifierTypeService.getIdentifierTypeName(productIdTypeId));
    } catch (Exception e) {
      log.warn("resolveIdentifierTypeName:: Cannot resolve identifier type '{}' for email context", productIdTypeId, e);
      return "";
    }
  }

  private String resolveContributorNameTypeName(String contributorNameTypeId) {
    try {
      return StringUtils.defaultString(contributorNameTypeService.getContributorNameTypeName(contributorNameTypeId));
    } catch (Exception e) {
      log.warn("resolveContributorNameTypeName:: Cannot resolve contributor name type '{}' for email context", contributorNameTypeId, e);
      return "";
    }
  }
}
