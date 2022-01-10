package org.folio.dew.batch.acquisitions.edifact;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.folio.dew.domain.dto.CompositePoLine;
import org.springframework.beans.factory.annotation.Autowired;

public class CompositePOLineConverter {
  private static final String DEFAULT_PUBLISHER = "Default Publisher";
  private static final String DEFAULT_PRODUCT_ID = "0393966518";
  private static final String DEFAULT_CONTRIBUTOR = "Stiglitz, Joseph E.";
  private static final String DEFAULT_TITLE = "Default Title";
  private static final String DEFAULT_PUBLICATION_DATE = "1999";
  private static final String DEFAULT_MATERIAL_TYPE = "Book";
  private static final String DEFAULT_PRICE = "49.99";

  @Autowired
  private MaterialTypeService materialTypeService;

  public int convertPOLine(CompositePoLine poLine, EDIStreamWriter writer, int currentLineNumber) throws EDIStreamException {
    int messageSegmentCount = 0;

    messageSegmentCount++;
    writeOrderLine(poLine, writer, currentLineNumber);

    messageSegmentCount++;
    writeProductID(poLine, writer);

    messageSegmentCount++;
    writeContributor(poLine, writer);

    messageSegmentCount++;
    writeTitle(poLine, writer);

    messageSegmentCount++;
    writePublisher(poLine, writer);

    messageSegmentCount++;
    writePublicationDate(poLine, writer);

    messageSegmentCount++;
    writeMaterialType(poLine, writer);

    messageSegmentCount++;
    writeQuantity(poLine, writer);

    messageSegmentCount++;
    writePrice(poLine, writer);

    messageSegmentCount++;
    writePOLineNumber(poLine, writer);

    messageSegmentCount++;
    writeFundCode(poLine, writer);

    messageSegmentCount++;
    writeDeliveryLocation(poLine, writer);

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
  private void writeContributor(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("009");

    String contributor = DEFAULT_CONTRIBUTOR;
    if (poLine.getContributors() != null && !poLine.getContributors()
      .isEmpty() && poLine.getContributors()
      .get(0)
      .getContributor() != null) {
      contributor = poLine.getContributors()
        .get(0)
        .getContributor();
    }
    writer.writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(contributor)
      .endElement()
      .writeEndSegment();
  }

  // Title
  private void writeTitle(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("050")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(poLine.getTitleOrPackage() != null ? poLine.getTitleOrPackage() : DEFAULT_TITLE)
      .endElement()
      .writeEndSegment();
  }

  // Publisher
  private void writePublisher(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("109")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(poLine.getPublisher() != null ? poLine.getPublisher() : DEFAULT_PUBLISHER)
      .endElement()
      .writeEndSegment();
  }

  // Publication date
  private void writePublicationDate(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD")
      .writeElement("L")
      .writeElement("170")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(poLine.getPublicationDate() != null ? poLine.getPublicationDate() : DEFAULT_PUBLICATION_DATE)
      .endElement()
      .writeEndSegment();
  }

  // Material type
  private void writeMaterialType(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("IMD");
    String materialTypeId;
    String material = DEFAULT_MATERIAL_TYPE;
    if (poLine.getPhysical() != null &&
      poLine.getPhysical().getMaterialType() != null) {
      materialTypeId = poLine.getPhysical().getMaterialType();
      material = materialTypeService.getMaterialTypeName(materialTypeId);
    } else if (poLine.getEresource() != null &&
    poLine.getEresource().getMaterialType() != null) {
      materialTypeId = poLine.getEresource().getMaterialType();
      material = materialTypeService.getMaterialTypeName(materialTypeId);
    }

    writer.writeElement("L")
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
  private void writeQuantity(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("QTY")
      .writeStartElement()
      .writeComponent("21")
      .writeComponent(String.valueOf(getQuantity(poLine))) // quantity value (always 1?)
      .endElement()
      .writeEndSegment();
  }

  // Price - Several different kinds of qualifiers may be used; see documentation
  private void writePrice(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("PRI");

    String price = DEFAULT_PRICE;
    if (poLine.getCost() != null && poLine.getCost()
      .getPoLineEstimatedPrice() != null) {
      price = String.valueOf(poLine.getCost()
        .getPoLineEstimatedPrice());
    }
    writer.writeStartElement()
      .writeComponent("AAB")
      .writeComponent(price)
      .endElement()
      .writeEndSegment();
  }

  // FOLIO POL
  private void writePOLineNumber(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("LI")
      .writeComponent(poLine.getPoLineNumber())
      .endElement()
      .writeEndSegment();
  }

  // FOLIO fund code (Also expense class?)
  private void writeFundCode(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("BFN")
      .writeComponent("LIBRARY")
      .endElement()
      .writeEndSegment();
  }

  // Delivery location
  private void writeDeliveryLocation(CompositePoLine poLine, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("LOC")
      .writeElement("20")
      .writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("92")
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

  private int getQuantity(CompositePoLine poLine) {
    int quantity = 0;
    if (poLine.getPhysical() != null && poLine.getCost() != null
      && poLine.getCost().getQuantityPhysical() != null) {
      quantity += poLine.getCost().getQuantityPhysical();
    }
    if (poLine.getEresource() != null && poLine.getCost() != null
      && poLine.getCost().getQuantityElectronic() != null){
      quantity += poLine.getCost().getQuantityElectronic();
    }
    return quantity;
  }
}
