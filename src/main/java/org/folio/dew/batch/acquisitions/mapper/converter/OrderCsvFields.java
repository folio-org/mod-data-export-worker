package org.folio.dew.batch.acquisitions.mapper.converter;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.folio.dew.domain.dto.Contributor;
import org.folio.dew.domain.dto.Cost;
import org.folio.dew.domain.dto.FundDistribution;
import org.folio.dew.domain.dto.ProductIdentifier;
import org.folio.dew.domain.dto.acquisitions.edifact.OrderCsvEntry;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getVendorAccountNumber;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getVendorOrderNumber;

@AllArgsConstructor
public enum OrderCsvFields implements ExtractableField<OrderCsvEntry, String> {

  POL_NUMBER("POL number", entry -> entry.poLine().getPoLineNumber()),
  ORDER_NUMBER("Order number", entry -> entry.order().getPoNumber()),
  VENDOR_ORDER_NUMBER("Vendor order number", entry -> getVendorOrderNumber(entry.poLine())),
  ACCOUNT_NUMBER("Account number", entry -> getVendorAccountNumber(entry.poLine())),
  TITLE("Title", entry -> entry.poLine().getTitleOrPackage()),
  PUBLISHER("Publisher", entry -> entry.poLine().getPublisher()),
  PUBLICATION_DATE("Publication date", entry -> entry.poLine().getPublicationDate()),
  PRODUCT_IDS("Product IDs", OrderCsvFields::extractProductIds),
  QUANTITY("Quantity", OrderCsvFields::extractQuantity),
  UNIT_PRICE("Unit price", OrderCsvFields::extractUnitPrice),
  ESTIMATED_PRICE("Estimated price", OrderCsvFields::extractEstimatedPrice),
  CURRENCY("Currency", entry -> Optional.ofNullable(entry.poLine().getCost()).map(Cost::getCurrency).orElse(null)),
  FUND_CODES("Fund codes", OrderCsvFields::extractFundCodes),
  CONTRIBUTORS("Contributors", OrderCsvFields::extractContributors),
  RUSH("Rush", entry -> Optional.ofNullable(entry.poLine().getRush()).map(String::valueOf).orElse(null));

  @Getter
  private final String name;
  private final Function<OrderCsvEntry, String> extractor;

  @Override
  public String extract(OrderCsvEntry entry) {
    return extractor.apply(entry);
  }

  private static String extractProductIds(OrderCsvEntry entry) {
    var details = entry.poLine().getDetails();
    if (details == null) {
      return null;
    }
    return joinList(details.getProductIds(), ProductIdentifier::getProductId);
  }

  private static String extractQuantity(OrderCsvEntry entry) {
    var cost = entry.poLine().getCost();
    if (cost == null) {
      return null;
    }
    int qty = Optional.ofNullable(cost.getQuantityPhysical()).orElse(0)
      + Optional.ofNullable(cost.getQuantityElectronic()).orElse(0);
    return String.valueOf(qty);
  }

  private static String extractUnitPrice(OrderCsvEntry entry) {
    var cost = entry.poLine().getCost();
    if (cost == null) {
      return null;
    }
    var price = Optional.ofNullable(cost.getListUnitPrice())
      .or(() -> Optional.ofNullable(cost.getListUnitPriceElectronic()));
    return price.map(String::valueOf).orElse(null);
  }

  private static String extractEstimatedPrice(OrderCsvEntry entry) {
    return Optional.ofNullable(entry.poLine().getCost())
      .map(Cost::getPoLineEstimatedPrice)
      .map(String::valueOf)
      .orElse(null);
  }

  private static String extractFundCodes(OrderCsvEntry entry) {
    return joinList(entry.poLine().getFundDistribution(), FundDistribution::getCode);
  }

  private static String extractContributors(OrderCsvEntry entry) {
    return joinList(entry.poLine().getContributors(), Contributor::getContributor);
  }

  private static <T> String joinList(List<T> list, Function<T, String> mapper) {
    if (list == null || list.isEmpty()) {
      return null;
    }
    return list.stream()
      .map(mapper)
      .filter(v -> v != null && !v.isBlank())
      .collect(Collectors.joining("; "));
  }

}
