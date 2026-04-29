package org.folio.dew.batch.acquisitions.mapper;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.acquisitions.utils.ExportUtils;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
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
public class OrderEmailContextMapper {

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
      .poNumber(order.getPoNumber())
      .orderDate(ExportUtils.getFormattedDate(order.getDateOrdered()))
      .orderType(Optional.ofNullable(order.getOrderType()).map(CompositePurchaseOrder.OrderTypeEnum::getValue).orElse(null))
      .createdBy(Optional.ofNullable(order.getMetadata()).map(m -> m.getCreatedByUsername()).orElse(null))
      .totalEstimatedPrice(formatDecimal(order.getTotalEstimatedPrice()))
      .shipTo(Optional.ofNullable(order.getShipTo()).map(Object::toString).orElse(null))
      .billTo(Optional.ofNullable(order.getBillTo()).map(Object::toString).orElse(null))
      .note(joinNotes(order.getNotes()))
      .build();
  }

  private OrderLineContext mapOrderLine(PoLine line) {
    var cost = line.getCost();
    var physQty = Optional.ofNullable(cost).map(Cost::getQuantityPhysical).orElse(0);
    var elecQty = Optional.ofNullable(cost).map(Cost::getQuantityElectronic).orElse(0);
    return OrderLineContext.builder()
      .poLineNumber(line.getPoLineNumber())
      .title(line.getTitleOrPackage())
      .publisher(line.getPublisher())
      .publicationDate(line.getPublicationDate())
      .edition(line.getEdition())
      .productIdentifier(mapProductIdentifiers(line.getDetails()))
      .materialType(resolveMaterialType(line.getPhysical(), line.getEresource()))
      .listUnitPrice(formatDecimal(Optional.ofNullable(cost).map(Cost::getListUnitPrice).orElse(null)))
      .listUnitPriceElectronic(formatDecimal(Optional.ofNullable(cost).map(Cost::getListUnitPriceElectronic).orElse(null)))
      .quantityPhysical(physQty)
      .quantityElectronic(elecQty)
      .quantity(physQty + elecQty)
      .estimatedPrice(formatDecimal(Optional.ofNullable(cost).map(Cost::getPoLineEstimatedPrice).orElse(null)))
      .currency(Optional.ofNullable(cost).map(Cost::getCurrency).orElse(null))
      .fundCodes(mapFundCodes(line.getFundDistribution()))
      .vendorRefNumber(mapVendorRefNumber(line.getVendorDetail()))
      .instructions(Optional.ofNullable(line.getVendorDetail()).map(VendorDetail::getInstructions).orElse(null))
      .build();
  }

  private String formatDecimal(BigDecimal value) {
    return value != null ? value.toPlainString() : null;
  }

  private String joinNotes(List<String> notes) {
    return CollectionUtils.isEmpty(notes) ? null : String.join("; ", notes);
  }

  private String mapProductIdentifiers(Details details) {
    if (details == null || CollectionUtils.isEmpty(details.getProductIds())) {
      return null;
    }
    return details.getProductIds().stream()
      .map(this::formatProductId)
      .collect(Collectors.joining("; "));
  }

  private String formatProductId(ProductIdentifier pid) {
    return StringUtils.isNotBlank(pid.getProductIdType())
      ? "%s (%s)".formatted(pid.getProductId(), pid.getProductIdType())
      : pid.getProductId();
  }

  private String resolveMaterialType(Physical physical, Eresource eresource) {
    if (physical != null && StringUtils.isNotBlank(physical.getMaterialType())) {
      return physical.getMaterialType();
    }
    if (eresource != null && StringUtils.isNotBlank(eresource.getMaterialType())) {
      return eresource.getMaterialType();
    }
    return null;
  }

  private String mapFundCodes(List<FundDistribution> fundDistributions) {
    if (CollectionUtils.isEmpty(fundDistributions)) {
      return null;
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
      .orElse(null);
  }
}
