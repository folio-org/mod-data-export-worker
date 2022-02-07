package org.folio.dew.batch.acquisitions.edifact;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import liquibase.util.StringUtil;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.Contributor;
import org.folio.dew.domain.dto.FundDistribution;
import org.folio.dew.domain.dto.Location;
import org.folio.dew.domain.dto.ReferenceNumberItem;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class CompositePOLineConverter {
  private static final int MAX_CHARS_PER_LINE = 70;
  private static final int MAX_NUMBER_OF_REFS = 10;
  private static final String DEFAULT_PRODUCT_ID = "0993966518";

  @Autowired
  private MaterialTypeService materialTypeService;

  public int convertPOLine(CompositePoLine poLine, EDIStreamWriter writer, int currentLineNumber, int quantityOrdered) throws EDIStreamException {
    int messageSegmentCount = 0;

    //TODO This should only be the 13 digit article number. Other identifiers should not be used here
    messageSegmentCount++;
    writeOrderLine(poLine, writer, currentLineNumber);

    //TODO repeat previous field with PIA+5+, repeat the field and use PIA+1+ for everything after the first one
    messageSegmentCount++;
    writeProductID(poLine, writer);

    for (Contributor contributor : poLine.getContributors()) {
      if (contributor.getContributor() != null) {
        messageSegmentCount++;
        writeContributor(contributor.getContributor(), writer);
      }
    }

    for (String titlePart : getTitleParts(poLine)) {
      messageSegmentCount++;
      writeTitle(titlePart, writer);
    }

    if (poLine.getPublisher() != null) {
      messageSegmentCount++;
      writePublisher(poLine.getPublisher(), writer);
    }

    if (poLine.getPublicationDate() != null) {
      messageSegmentCount++;
      writePublicationDate(poLine.getPublicationDate(), writer);
    }

    String physicalMaterial = getPhysicalMaterial(poLine);
    if (StringUtil.isNotEmpty(physicalMaterial)) {
      messageSegmentCount++;
      writeMaterialType(physicalMaterial, writer);
    }

    String electronicMaterial = getElectronicMaterial(poLine);
    if (StringUtil.isNotEmpty(electronicMaterial)) {
      messageSegmentCount++;
      writeMaterialType(electronicMaterial, writer);
    }

    messageSegmentCount++;
    writeQuantity(quantityOrdered, writer);

    if (poLine.getCost().getPoLineEstimatedPrice() != null) {
      messageSegmentCount++;
      writePrice(poLine.getCost().getPoLineEstimatedPrice(), writer);
    }

    messageSegmentCount++;
    writePoLineCurrency(poLine, writer);

    if (poLine.getVendorDetail() != null && StringUtil.isNotEmpty(poLine.getVendorDetail().getInstructions())) {
      messageSegmentCount++;
      writeInstructionsToVendor(poLine.getVendorDetail().getInstructions(), writer);
    }

    int referenceQuantity = 0;

    referenceQuantity++;
    messageSegmentCount++;
    writePOLineNumber(poLine, writer);

    for (FundDistribution fundDistribution : poLine.getFundDistribution()) {
      if (referenceQuantity >= MAX_NUMBER_OF_REFS) {
        //TODO show a warning
        break;
      }
      referenceQuantity++;
      messageSegmentCount++;
      writeFundCode(fundDistribution, writer);
    }

    if (poLine.getVendorDetail() != null && referenceQuantity < MAX_NUMBER_OF_REFS) {
      for (ReferenceNumberItem number : poLine.getVendorDetail().getReferenceNumbers()) {
        if (referenceQuantity >= MAX_NUMBER_OF_REFS) {
          //TODO show a warning
          break;
        }
        if (number.getRefNumber() != null) {
          referenceQuantity++;
          messageSegmentCount++;
          writeVendorReferenceNumber(number.getRefNumber(), writer);
        }
      }
    }

    for (Location location : poLine.getLocations()) {
      messageSegmentCount++;
      writeDeliveryLocation(location, writer);
    }

    return messageSegmentCount;
  }

  // Product ID:ID type (EAN/UPC/barcode number)
  // May or may not be the same number as the POL line number, since the POLs may skip numbers, and the EDI order does not
  // Since ISBNs expanded to 13 digits, they are equivalent to UPC barcode numbers
  // Identifier qualifier not included
  private void writeOrderLine(CompositePoLine poLine, EDIStreamWriter writer, int currentLineNumber) throws EDIStreamException {
    writer.writeStartSegment("LIN")
      .writeElement(String.valueOf(currentLineNumber))
      .writeElement("")
      .writeStartElement()
      .writeComponent(getProductID(poLine))
      .writeComponent("EN")
      .endElement()
      .writeEndSegment();
  }

  // Product ID:ID type
  // ISBN-10 is no longer necessary
  // PIA includes whatever Product IDs are in the POL, not just an ISBN
  private void writeProductID(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("PIA")
      .writeElement("5")
      .writeStartElement()
      .writeComponent(getProductID(poLine))
      .writeComponent("IB")
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

  private void writePoLineCurrency(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("CUX")
      .writeStartElement()
      .writeComponent("2")
      .writeComponent(poLine.getCost().getCurrency())
      .writeComponent("9")
      .endElement()
      .writeEndSegment();
  }

  private void writeInstructionsToVendor(String instructions, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("FTX")
      .writeElement("LIN")
      .writeElement("")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent(instructions)
      .endElement()
      .writeEndSegment();
  }

  private String[] getTitleParts(CompositePoLine poLine) {
    String title = poLine.getTitleOrPackage();
    return title.split("(?<=\\G.{" + MAX_CHARS_PER_LINE + "})");
  }

  private void writePOLineNumber(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("LI")
      .writeComponent(poLine.getPoLineNumber())
      .endElement()
      .writeEndSegment();
  }

  // FOLIO fund code and expense class
  private void writeFundCode(FundDistribution fundDistribution, EDIStreamWriter writer) throws EDIStreamException {
    //TODO Make a call to get expense class by ID fundDistribution.getExpenseClassId()
    //String fundCodeWithExpenseClass = fundDistribution.getCode() + "?:" + expenseClass;
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("BFN")
      .writeComponent(fundDistribution.getCode())//fundCodeWithExpenseClass
      .endElement()
      .writeEndSegment();
  }

  private void writeVendorReferenceNumber(String number, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("SLI")
      .writeComponent(number)
      .endElement()
      .writeEndSegment();
  }

  private void writeDeliveryLocation(Location location, EDIStreamWriter writer) throws EDIStreamException {
    //TODO Make a call to get location code for the ID found here (for holding we still need to get location code, not the holding code)
    writer.writeStartSegment("LOC")
      .writeElement("20")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("92")//locationCode
      .endElement()
      .writeEndSegment();
  }

  private String getProductID(CompositePoLine poLine) {
    String productId = DEFAULT_PRODUCT_ID;
    if (poLine.getDetails() != null && poLine.getDetails()
      .getProductIds() != null && !poLine.getDetails()
      .getProductIds()
      .isEmpty() && poLine.getDetails()
      .getProductIds()
      .get(0)
      .getProductId() != null) {
      productId = poLine.getDetails()
        .getProductIds()
        .get(0)
        .getProductId();
    }
    return productId;
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
}
