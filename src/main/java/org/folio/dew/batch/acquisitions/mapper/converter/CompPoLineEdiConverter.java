package org.folio.dew.batch.acquisitions.mapper.converter;

import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getVendorOrderNumber;
import static org.folio.dew.batch.acquisitions.utils.ExportUtils.getVendorReferenceNumbers;
import static org.folio.dew.domain.dto.VendorEdiOrdersExportConfig.IntegrationTypeEnum.CLAIMING;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import one.util.streamex.StreamEx;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.acquisitions.services.ExpenseClassService;
import org.folio.dew.batch.acquisitions.services.HoldingService;
import org.folio.dew.batch.acquisitions.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.services.LocationService;
import org.folio.dew.batch.acquisitions.services.MaterialTypeService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.Contributor;
import org.folio.dew.domain.dto.Cost;
import org.folio.dew.domain.dto.FundDistribution;
import org.folio.dew.domain.dto.Location;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.ProductIdentifier;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompPoLineEdiConverter {
  private static final int MAX_CHARS_PER_LINE = 70;
  private static final int MAX_NUMBER_OF_REFS = 10;
  private static final String PRODUCT_ID_FUNCTION_CODE_MAIN_PRODUCT_IDNTIFICATION = "5";
  private static final String PRODUCT_ID_FUNCTION_CODE_ADDITIONAL_IDNTIFICATION = "1";
  private static final String FUND_CODE_EXPENSE_CLASS_SEPARATOR = ":";
  private static final String ISBN_PRODUCT_ID_TYPE = "ISBN";
  private static final String ISSN_PRODUCT_ID_TYPE = "ISSN";
  private static final String ISMN_PRODUCT_ID_TYPE = "ISMN";
  private static final String IB_PRODUCT_ID_QUALIFIER = "IB";
  private static final String IS_PRODUCT_ID_QUALIFIER = "IS";
  private static final String IM_PRODUCT_ID_QUALIFIER = "IM";
  private static final String MF_PRODUCT_ID_QUALIFIER = "MF";
  private static final String EN_PRODUCT_ID_QUALIFIER = "EN";
  private static final int EAN_IDENTIFIER_LENGTH = 13;

  private final IdentifierTypeService identifierTypeService;
  private final MaterialTypeService materialTypeService;
  private final ExpenseClassService expenseClassService;
  private final LocationService locationService;
  private final HoldingService holdingService;

  public CompPoLineEdiConverter(IdentifierTypeService identifierTypeService, MaterialTypeService materialTypeService,
                                ExpenseClassService expenseClassService, LocationService locationService, HoldingService holdingService) {
    this.identifierTypeService = identifierTypeService;
    this.materialTypeService = materialTypeService;
    this.expenseClassService = expenseClassService;
    this.locationService = locationService;
    this.holdingService = holdingService;
  }

  public int convertPOLine(VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType,
                           CompositePurchaseOrder compPO, CompositePoLine poLine, List<Piece> pieces, EDIStreamWriter writer,
                           int currentLineNumber, int quantityOrdered) throws EDIStreamException {
    int messageSegmentCount = 0;

    Map<String, ProductIdentifier> productTypeProductIdentifierMap = prepareStringProductIdentifierMap(poLine);
    messageSegmentCount += writeOrderLineAndMainProductId(productTypeProductIdentifierMap, writer, currentLineNumber);
    messageSegmentCount = writeProductIdentifiers(writer, productTypeProductIdentifierMap, messageSegmentCount);
    messageSegmentCount = writeContributors(poLine, writer, messageSegmentCount);
    messageSegmentCount = writeTitles(poLine, writer, messageSegmentCount);
    messageSegmentCount = writePieces(pieces, writer, messageSegmentCount);

    if (poLine.getPublisher() != null) {
      messageSegmentCount++;
      writePublisher(poLine.getPublisher(), writer);
    }

    if (poLine.getPublicationDate() != null) {
      messageSegmentCount++;
      writePublicationDate(poLine.getPublicationDate(), writer);
    }

    String physicalMaterial = getPhysicalMaterial(poLine);
    if (StringUtils.isNotEmpty(physicalMaterial)) {
      messageSegmentCount++;
      writeMaterialType(physicalMaterial, writer);
    }

    String electronicMaterial = getElectronicMaterial(poLine);
    if (StringUtils.isNotEmpty(electronicMaterial)) {
      messageSegmentCount++;
      writeMaterialType(electronicMaterial, writer);
    }

    if (integrationType == CLAIMING) {
      writeCurrentStatus(writer);
    }

    messageSegmentCount++;
    writeQuantity(quantityOrdered, writer);

    if (poLine.getCost().getPoLineEstimatedPrice() != null) {
      messageSegmentCount++;
      writePrice(poLine.getCost().getPoLineEstimatedPrice(), writer);
    }

    if (poLine.getCost().getListUnitPrice() != null || poLine.getCost().getListUnitPriceElectronic() != null) {
      messageSegmentCount++;
      String totalUnitPrice = calculateCostUnitsTotal(poLine.getCost());
      writeUnitPrice(totalUnitPrice, writer);
    }

    messageSegmentCount++;
    writePoLineCurrency(poLine, writer);

    if (integrationType == CLAIMING) {
      writePONumber(compPO, writer);
    }

    if (poLine.getVendorDetail() != null && StringUtils.isNotEmpty(poLine.getVendorDetail().getInstructions())) {
      messageSegmentCount++;
      writeInstructionsToVendor(poLine.getVendorDetail().getInstructions(), writer);
    }

    int referenceQuantity = 0;

    referenceQuantity++;
    messageSegmentCount++;
    writePOLineNumber(poLine, writer);
    if (integrationType == CLAIMING) {
      writeClaims(writer);
    }

    var fundDistributions = getWriteFundDistributions(poLine, writer, referenceQuantity, messageSegmentCount);
    messageSegmentCount = writeVendorOrderNumber(poLine, pieces, writer, fundDistributions.messageSegmentCount());
    messageSegmentCount = writeVendorReferenceNumbers(poLine, writer, fundDistributions.referenceQuantity(), messageSegmentCount);
    messageSegmentCount = writeDeliveryLocations(poLine, writer, messageSegmentCount);

    return messageSegmentCount;
  }

  private  Map<String, ProductIdentifier> prepareStringProductIdentifierMap(CompositePoLine poLine) {
    var productTypeProductIdentifierMap = new HashMap<String, ProductIdentifier>();
    for (var productId : getProductIds(poLine)) {
      productTypeProductIdentifierMap.put(getProductIdType(productId), productId);
    }
    return productTypeProductIdentifierMap;
  }

  private int writeProductIdentifiers(EDIStreamWriter writer, Map<String, ProductIdentifier> productTypeProductIdentifierMap,
                                      int messageSegmentCount) throws EDIStreamException {
    for (var entry : productTypeProductIdentifierMap.entrySet()) {
      messageSegmentCount++;
      writeProductId(entry.getValue().getProductId(), writer, PRODUCT_ID_FUNCTION_CODE_ADDITIONAL_IDNTIFICATION, getProductIdQualifier(entry.getKey()));
    }
    return messageSegmentCount;
  }

  private int writeTitles(CompositePoLine poLine, EDIStreamWriter writer,
                          int messageSegmentCount) throws EDIStreamException {
    for (var titlePart : getTitleParts(poLine)) {
      messageSegmentCount++;
      writeTitle(titlePart, writer);
    }
    return messageSegmentCount;
  }

  private int writePieces(List<Piece> pieces, EDIStreamWriter writer,
                          int messageSegmentCount) throws EDIStreamException {
    for (var piece : pieces) {
      var pieceDetails = getPieceDetails(piece);
      if (StringUtils.isNotBlank(pieceDetails)) {
        messageSegmentCount++;
        writePiece(pieceDetails, writer);
      }
    }
    return messageSegmentCount;
  }

  private int writeContributors(CompositePoLine poLine, EDIStreamWriter writer,
                                int messageSegmentCount) throws EDIStreamException {
    for (var contributor : getContributors(poLine)) {
      if (contributor.getContributor() != null) {
        messageSegmentCount++;
        writeContributor(contributor.getContributor(), writer);
      }
    }
    return messageSegmentCount;
  }

  private CompPoLineEdiConverter.PreparedFundDistributions getWriteFundDistributions(CompositePoLine poLine, EDIStreamWriter writer,
                                                                                 int referenceQuantity, int messageSegmentCount) throws EDIStreamException {
    for (var fundDistribution : getFundDistribution(poLine)) {
      if (referenceQuantity >= MAX_NUMBER_OF_REFS) {
        break;
      }
      referenceQuantity++;
      messageSegmentCount++;
      writeFundCode(getFundAndExpenseClass(fundDistribution), writer);
    }
    return new PreparedFundDistributions(messageSegmentCount, referenceQuantity);
  }

  private record PreparedFundDistributions(int messageSegmentCount, int referenceQuantity) {
  }

  private int writeVendorOrderNumber(CompositePoLine poLine, List<Piece> pieces, EDIStreamWriter writer,
                                     int messageSegmentCount) throws EDIStreamException {
    var referenceNumbers = getVendorReferenceNumbers(poLine);
    // Non-empty pieces list is a sign that the export is for claims
    if (CollectionUtils.isNotEmpty(pieces)) {
      // Vendor order number is a required field for claims export, it must be included regardless of the number of references
      var vendorOrderNumber = getAndRemoveVendorOrderNumber(referenceNumbers);
      if (vendorOrderNumber != null) {
        messageSegmentCount++;
        writeVendorOrderNumber(vendorOrderNumber.getRefNumber(), writer);
      }
    }
    return messageSegmentCount;
  }

  private int writeVendorReferenceNumbers(CompositePoLine poLine, EDIStreamWriter writer,
                                          int referenceQuantity, int messageSegmentCount) throws EDIStreamException {
    for (var number : getVendorReferenceNumbers(poLine)) {
      if (referenceQuantity >= MAX_NUMBER_OF_REFS) {
        break;
      }
      if (number.getRefNumber() != null) {
        referenceQuantity++;
        messageSegmentCount++;
        writeVendorReferenceNumber(number.getRefNumber(), writer);
      }
    }
    return messageSegmentCount;
  }

  private int writeDeliveryLocations(CompositePoLine poLine, EDIStreamWriter writer,
                                     int messageSegmentCount) throws EDIStreamException {
    for (var location : getLocations(poLine)) {
      messageSegmentCount++;
      writeDeliveryLocation(getLocationCode(location), writer);
    }
    return messageSegmentCount;
  }

  private int writeOrderLineAndMainProductId(Map<String, ProductIdentifier> productTypeProductIdentifierMap, EDIStreamWriter writer,
                                             int currentLineNumber) throws EDIStreamException {
    int numberOfLinesWritten;
    if (productTypeProductIdentifierMap.get(ISBN_PRODUCT_ID_TYPE) != null) {
      writeMainProduct(productTypeProductIdentifierMap, writer, currentLineNumber, IB_PRODUCT_ID_QUALIFIER, ISBN_PRODUCT_ID_TYPE);
      numberOfLinesWritten = 2;
    } else if (productTypeProductIdentifierMap.get(ISSN_PRODUCT_ID_TYPE) != null) {
      writeMainProduct(productTypeProductIdentifierMap, writer, currentLineNumber, IS_PRODUCT_ID_QUALIFIER, ISSN_PRODUCT_ID_TYPE);
      numberOfLinesWritten = 2;
    } else if (productTypeProductIdentifierMap.get(ISMN_PRODUCT_ID_TYPE) != null) {
      writeMainProduct(productTypeProductIdentifierMap, writer, currentLineNumber, IM_PRODUCT_ID_QUALIFIER, ISMN_PRODUCT_ID_TYPE);
      numberOfLinesWritten = 2;
    } else {
      writeOrderLine("", writer, currentLineNumber, "");
      numberOfLinesWritten = 1;
    }
    return numberOfLinesWritten;
  }

  private void writeMainProduct(Map<String, ProductIdentifier> productTypeProductIdentifierMap, EDIStreamWriter writer,
                                int currentLineNumber, String qualifier, String productType) throws EDIStreamException {
    writeOrderLine(productTypeProductIdentifierMap.get(productType).getProductId(), writer, currentLineNumber, qualifier);
    writeProductId(productTypeProductIdentifierMap.get(productType).getProductId(), writer, PRODUCT_ID_FUNCTION_CODE_MAIN_PRODUCT_IDNTIFICATION, qualifier);
    productTypeProductIdentifierMap.remove(productType);
  }

  // Product ID:ID type (EAN/UPC/barcode number)
  // May or may not be the same number as the POL line number, since the POLs may skip numbers, and the EDI order does not
  // Since ISBNs expanded to 13 digits, they are equivalent to UPC barcode numbers
  // Identifier qualifier not included
  private void writeOrderLine(String productId, EDIStreamWriter writer, int currentLineNumber, String qualifier) throws EDIStreamException {
    String linQualifier = productId != null && productId.length() == EAN_IDENTIFIER_LENGTH
      ? EN_PRODUCT_ID_QUALIFIER : qualifier;
    writer.writeStartSegment("LIN")
      .writeElement(String.valueOf(currentLineNumber))
      .writeElement("")
      .writeStartElement()
      .writeComponent(productId)
      .writeComponent(linQualifier)
      .endElement()
      .writeEndSegment();
  }

  // Product ID:ID type
  // ISBN-10 is no longer necessary
  // PIA includes whatever Product IDs are in the POL, not just an ISBN
  private void writeProductId(String productId, EDIStreamWriter writer, String productIdFunctionCode, String qualifier) throws EDIStreamException {
    writer.writeStartSegment("PIA")
      .writeElement(productIdFunctionCode)
      .writeStartElement()
      .writeComponent(productId)
      .writeComponent(qualifier)
      .endElement()
      .writeEndSegment();
  }

  // Author (Contributor)
  private void writeContributor(String contributor, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("009")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(contributor)
      .endElement()
      .writeEndSegment();
  }

  private void writeTitle(String titlePart, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("050")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(titlePart)
      .endElement()
      .writeEndSegment();
  }

  private void writePiece(String pieceDetails, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("080")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(pieceDetails)
      .endElement()
      .writeEndSegment();
  }

  private void writePublisher(String publisher, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("109")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(publisher)
      .endElement()
      .writeEndSegment();
  }

  private void writePublicationDate(String publicationDate, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("170")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(publicationDate)
      .endElement()
      .writeEndSegment();
  }

  private void writeMaterialType(String material, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("180")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(material)
      .endElement()
      .writeEndSegment();
  }

  private void writeCurrentStatus(EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("STS")
      .writeElement("UP1")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("55")
      .writeComponent("CSD")
      .endElement()
      .writeEndSegment();
  }

  // Quantity ordered
  private void writeQuantity(int quantityOrdered, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("QTY")
      .writeStartElement()
      .writeComponent("21")
      .writeComponent(String.valueOf(quantityOrdered))
      .endElement()
      .writeEndSegment();
  }

  private void writePrice(BigDecimal price, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("PRI")
      .writeStartElement()
      .writeComponent("AAF")
      .writeComponent(String.valueOf(price))
      .endElement()
      .writeEndSegment();
  }

  private void writeUnitPrice(String price, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("PRI")
      .writeStartElement()
      .writeComponent("AAB")
      .writeComponent(price)
      .endElement()
      .writeEndSegment();
  }

  private void writePoLineCurrency(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("CUX")
      .writeStartElement()
      .writeComponent("2")
      .writeComponent(poLine.getCost().getCurrency())
      .writeComponent("9")
      .endElement()
      .writeEndSegment();
  }

  private void writePONumber(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeElement("SNA")
      .writeElement(compPO.getPoNumber())
      .writeEndSegment();
  }

  private void writeInstructionsToVendor(String instructions, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("FTX")
      .writeElement("LIN")
      .writeElement("")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .endElement()
      .writeStartElement()
      .writeComponent(instructions)
      .endElement()
      .writeEndSegment();
  }

  private void writePOLineNumber(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("LI")
      .writeComponent(poLine.getPoLineNumber())
      .endElement()
      .writeEndSegment();
  }

  private void writeClaims(EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeElement("ACT")
      .writeElement("1")
      .writeEndSegment();
  }

  // FOLIO fund code and expense class
  private void writeFundCode(String fundAndExpenseClass, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("BFN")
      .writeComponent(fundAndExpenseClass)
      .endElement()
      .writeEndSegment();
  }

  private void writeVendorOrderNumber(String number, EDIStreamWriter writer) throws EDIStreamException {
    writeVendorReferenceNumber(number, "SNA", writer);
  }

  private void writeVendorReferenceNumber(String number, EDIStreamWriter writer) throws EDIStreamException {
    writeVendorReferenceNumber(number, "SLI", writer);
  }

  private void writeVendorReferenceNumber(String number, String component, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent(component)
      .writeComponent(number)
      .endElement()
      .writeEndSegment();
  }

  private void writeDeliveryLocation(String locationCode, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("LOC")
      .writeElement("7")
      .writeStartElement()
      .writeComponent(locationCode)
      .writeComponent("")
      .writeComponent("92")
      .endElement()
      .writeEndSegment();
  }

  private List<ProductIdentifier> getProductIds(CompositePoLine poLine) {
    if (poLine.getDetails() != null && poLine.getDetails().getProductIds() != null) {
      return poLine.getDetails().getProductIds();
    } else {
      return new ArrayList<>();
    }
  }

  private String getProductIdType(ProductIdentifier productId) {
    if (productId.getProductIdType() != null) {
      return identifierTypeService.getIdentifierTypeName(productId.getProductIdType());
    }
    return "";
  }

  private String getProductIdQualifier(String productIdType) {
    return switch (productIdType) {
      case "ISBN" -> IB_PRODUCT_ID_QUALIFIER;
      case "ISSN" -> IS_PRODUCT_ID_QUALIFIER;
      case "ISMN" -> IM_PRODUCT_ID_QUALIFIER;
      default -> MF_PRODUCT_ID_QUALIFIER;
    };
  }

  private List<Contributor> getContributors(CompositePoLine poLine) {
    if (poLine.getContributors() != null) {
      return poLine.getContributors();
    } else {
      return new ArrayList<>();
    }
  }

  private String[] getTitleParts(CompositePoLine poLine) {
    String title = poLine.getTitleOrPackage();
    return title.split("(?<=\\G.{" + MAX_CHARS_PER_LINE + "})");
  }

  private String getPieceDetails(Piece piece) {
    return StreamEx.of(piece.getDisplaySummary(), piece.getChronology(), piece.getEnumeration())
      .filter(StringUtils::isNotBlank)
      .joining(":");
  }

  private String getPhysicalMaterial(CompositePoLine poLine) {
    if (poLine.getPhysical() != null && poLine.getPhysical().getMaterialType() != null) {
      String materialTypeId = poLine.getPhysical().getMaterialType();
      return materialTypeService.getMaterialTypeName(materialTypeId);
    }
    return "";
  }

  private String getElectronicMaterial(CompositePoLine poLine) {
     if (poLine.getEresource() != null && poLine.getEresource().getMaterialType() != null) {
      String materialTypeId = poLine.getEresource().getMaterialType();
      return materialTypeService.getMaterialTypeName(materialTypeId);
    }
    return "";
  }

  private List<FundDistribution> getFundDistribution(CompositePoLine poLine) {
    if (poLine.getFundDistribution() != null) {
      return poLine.getFundDistribution();
    }
    return new ArrayList<>();
  }

  private String getFundAndExpenseClass(FundDistribution fundDistribution) {
    String fundCode = fundDistribution.getCode();
    String expenseClass = getExpenseClassCode(fundDistribution);
    if (expenseClass.isEmpty()) {
      return fundCode;
    }
    return fundCode + FUND_CODE_EXPENSE_CLASS_SEPARATOR + expenseClass;
  }

  private String getExpenseClassCode(FundDistribution fundDistribution) {
    if (fundDistribution.getExpenseClassId() != null) {
      String expenseClassId = fundDistribution.getExpenseClassId().toString();
      return expenseClassService.getExpenseClassCode(expenseClassId);
    }
    return "";
  }

  private ReferenceNumberItem getAndRemoveVendorOrderNumber(List<ReferenceNumberItem> referenceNumberItems) {
    var vendorOrderNumber = getVendorOrderNumber(referenceNumberItems);
    referenceNumberItems.remove(vendorOrderNumber);
    return vendorOrderNumber;
  }

  private List<Location> getLocations(CompositePoLine poLine) {
    if (poLine.getLocations() != null) {
      return poLine.getLocations();
    }
    return new ArrayList<>();
  }

  private String getLocationCode(Location location) {
    if (location.getLocationId() != null) {
      return locationService.getLocationCodeById(location.getLocationId());
    } else if (location.getHoldingId() != null) {
      String locationId = holdingService.getPermanentLocationByHoldingId(location.getHoldingId().toString());
      return locationService.getLocationCodeById(locationId);
    }
    return "";
  }


  /**
   * The method is using calculation that similar to calculation in mod-order (HelperUtils -> calculateCostUnitsTotal),
   * but without additional cost and discount.
   *
   * @param cost Cost object of ComPoLine
   * @return unit price without discount and additional cost
   */
  private String calculateCostUnitsTotal(Cost cost) {
    CurrencyUnit currency = Monetary.getCurrency(cost.getCurrency());
    MonetaryAmount total = Money.of(0, currency);

    // Physical resources price
    if (cost.getListUnitPrice() != null && cost.getQuantityPhysical() != null) {
      MonetaryAmount pPrice = Money.of(cost.getListUnitPrice(), currency)
        .multiply(cost.getQuantityPhysical());
      total = total.add(pPrice);
    }

    // Electronic resources price
    if (cost.getListUnitPriceElectronic() != null && cost.getQuantityElectronic() != null) {
      MonetaryAmount ePrice = Money.of(cost.getListUnitPriceElectronic(), currency)
        .multiply(cost.getQuantityElectronic());
      total = total.add(ePrice);
    }

    return total.toString();
  }
}
