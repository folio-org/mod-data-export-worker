package org.folio.dew.batch.acquisitions.mapper;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.acquisitions.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.utils.ExportUtils;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Contributor;
import org.folio.dew.domain.dto.Cost;
import org.folio.dew.domain.dto.Details;
import org.folio.dew.domain.dto.Eresource;
import org.folio.dew.domain.dto.FundDistribution;
import org.folio.dew.domain.dto.Physical;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.ProductIdentifier;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.folio.dew.domain.dto.VendorDetail;
import org.folio.dew.domain.dto.templateengine.OrderContext;
import org.folio.dew.domain.dto.templateengine.OrderEmailContext;
import org.folio.dew.domain.dto.templateengine.OrderLineContext;
import org.folio.dew.domain.dto.templateengine.OrderLineWrapper;
import org.folio.dew.domain.dto.templateengine.OrderWrapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderEmailContextMapper {

  private final IdentifierTypeService identifierTypeService;
  private final ConfigurationService configurationService;

  public OrderEmailContext buildContext(List<CompositePurchaseOrder> orders) {
    var orderWrappers = orders.stream()
      .map(order -> new OrderWrapper(
        mapOrder(order),
        order.getPoLines().stream()
          .map(line -> new OrderLineWrapper(mapOrderLine(line)))
          .toList()))
      .toList();
    return OrderEmailContext.builder().orders(orderWrappers).build();
  }

  private OrderContext mapOrder(CompositePurchaseOrder order) {
    return OrderContext.builder()
      .poNumber(StringUtils.defaultString(order.getPoNumber()))
      .orderDate(StringUtils.defaultString(ExportUtils.getFormattedDate(order.getDateOrdered())))
      .orderType(Optional.ofNullable(order.getOrderType()).map(CompositePurchaseOrder.OrderTypeEnum::getValue).orElse(""))
      .createdBy(Optional.ofNullable(order.getMetadata()).map(m -> m.getCreatedByUsername()).orElse(""))
      .totalEstimatedPrice(formatDecimal(order.getTotalEstimatedPrice()))
      .shipTo(configurationService.getAddressConfig(order.getShipTo()))
      .billTo(configurationService.getAddressConfig(order.getBillTo()))
      .notes(joinNotes(order.getNotes()))
      .build();
  }

  private OrderLineContext mapOrderLine(PoLine line) {
    var cost = line.getCost();
    var physQty = Optional.ofNullable(cost).map(Cost::getQuantityPhysical).orElse(0);
    var elecQty = Optional.ofNullable(cost).map(Cost::getQuantityElectronic).orElse(0);
    return OrderLineContext.builder()
      .poLineNumber(StringUtils.defaultString(line.getPoLineNumber()))
      .title(StringUtils.defaultString(line.getTitleOrPackage()))
      .contributors(mapContributors(line.getContributors()))
      .publisher(StringUtils.defaultString(line.getPublisher()))
      .publicationDate(StringUtils.defaultString(line.getPublicationDate()))
      .edition(StringUtils.defaultString(line.getEdition()))
      .productIdentifier(mapProductIdentifiers(line.getDetails()))
      .productIdentifierType(mapProductIdentifierTypes(line.getDetails()))
      .materialType(resolveMaterialType(line.getPhysical(), line.getEresource()))
      .listUnitPrice(formatDecimal(Optional.ofNullable(cost).map(Cost::getListUnitPrice).orElse(null)))
      .listUnitPriceElectronic(formatDecimal(Optional.ofNullable(cost).map(Cost::getListUnitPriceElectronic).orElse(null)))
      .quantityPhysical(physQty)
      .quantityElectronic(elecQty)
      .quantity(physQty + elecQty)
      .estimatedPrice(formatDecimal(Optional.ofNullable(cost).map(Cost::getPoLineEstimatedPrice).orElse(null)))
      .currency(Optional.ofNullable(cost).map(Cost::getCurrency).orElse(""))
      .fundCodes(mapFundCodes(line.getFundDistribution()))
      .vendorRefNumber(mapVendorRefNumber(line.getVendorDetail()))
      .instructions(Optional.ofNullable(line.getVendorDetail()).map(VendorDetail::getInstructions).orElse(""))
      .build();
  }

  private String formatDecimal(BigDecimal value) {
    return value != null ? value.toPlainString() : "";
  }

  private String joinNotes(List<String> notes) {
    return CollectionUtils.isEmpty(notes) ? "" : String.join("; ", notes);
  }

  private String mapContributors(List<Contributor> contributors) {
    if (CollectionUtils.isEmpty(contributors)) {
      return "";
    }
    return contributors.stream()
      .map(Contributor::getContributor)
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining("; "));
  }

  private String mapProductIdentifiers(Details details) {
    if (details == null || CollectionUtils.isEmpty(details.getProductIds())) {
      return "";
    }
    return details.getProductIds().stream()
      .map(ProductIdentifier::getProductId)
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining("; "));
  }

  private String mapProductIdentifierTypes(Details details) {
    if (details == null || CollectionUtils.isEmpty(details.getProductIds())) {
      return "";
    }
    return details.getProductIds().stream()
      .map(ProductIdentifier::getProductIdType)
      .filter(StringUtils::isNotBlank)
      .map(identifierTypeService::getIdentifierTypeName)
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining("; "));
  }

  private String resolveMaterialType(Physical physical, Eresource eresource) {
    if (physical != null && StringUtils.isNotBlank(physical.getMaterialType())) {
      return physical.getMaterialType();
    }
    if (eresource != null && StringUtils.isNotBlank(eresource.getMaterialType())) {
      return eresource.getMaterialType();
    }
    return "";
  }

  private String mapFundCodes(List<FundDistribution> fundDistributions) {
    if (CollectionUtils.isEmpty(fundDistributions)) {
      return "";
    }
    return fundDistributions.stream()
      .map(FundDistribution::getCode)
      .filter(StringUtils::isNotBlank)
      .collect(Collectors.joining(", "));
  }

  private String mapVendorRefNumber(VendorDetail vendorDetail) {
    return Optional.ofNullable(vendorDetail)
      .map(VendorDetail::getReferenceNumbers)
      .filter(refs -> !refs.isEmpty())
      .map(refs -> refs.get(0))
      .map(ReferenceNumberItem::getRefNumber)
      .orElse("");
  }
}
