package org.folio.dew.batch.acquisitions.edifact;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

public class OrderEdifactExporter {

  public String convertOrdersToEdi(List<CompositePurchaseOrder> compPOs) throws EDIStreamException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    EDIOutputFactory factory = EDIOutputFactory.newFactory();
    factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);

    EDIStreamWriter writer = factory.createEDIStreamWriter(stream);

    // Count of messages (one message per purchase order)
    int messageCount = 0;

    writer.startInterchange();

    // Start of file - Can contain multiple order messages
    writer.writeStartSegment("UNA")
      .writeEndSegment();

    // Interchange header (Library ID:ID type; Vendor ID:ID type)
    writer.writeStartSegment("UNB");

    writer.writeStartElement()
      .writeComponent("UNOC")
      .writeComponent("1")
      .endElement();

    writer.writeStartElement()
      .writeComponent("901494200")
      .writeComponent("31B")
      .endElement();

    writer.writeElement("1001");

    writer.writeEndSegment();

    // Purchase orders
    for (CompositePurchaseOrder compPO : compPOs) {
      convertPO(compPO, writer);
      messageCount++;
    }

    // Interchange trailer (aka end of file)
    writer.writeStartSegment("UNZ")
      .writeElement(String.valueOf(messageCount))
      .writeElement("1001")
      .writeEndSegment();

    writer.endInterchange();

    writer.close();

    return stream.toString();
  }

  private void convertPO(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    // Count of number of segments in a message including the message header segment (UNH) and the message trailer segment (UNT)
    int messageSegmentCount = 0;

    // Order header = Start of order; EDIFACT message type - There would be a new UNH for each FOLIO PO in the file
    messageSegmentCount++;
    writer.writeStartSegment("UNH");

    writer.writeElement("1001");

    writer.writeStartElement()
      .writeComponent("ORDERS")
      .writeComponent("D")
      .writeComponent("96A")
      .writeComponent("UN")
      .writeComponent("EAN008")
      .endElement();

    writer.writeEndSegment();

    // FOLIO PO
    messageSegmentCount++;
    writer.writeStartSegment("BGM");
    writer.writeElement("220");
    writer.writeElement(compPO.getPoNumber());
    writer.writeElement("9");
    writer.writeEndSegment();

    // Order date:Date format
    messageSegmentCount++;
    writer.writeStartSegment("DTM");

    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    writer.writeStartElement()
      .writeComponent("137")
      .writeComponent(compPO.getDateOrdered() != null ? dateFormat.format(compPO.getDateOrdered()) : "20130620")
      .writeComponent("102")
      .endElement();

    writer.writeEndSegment();

    // Library ID and ID type
    messageSegmentCount++;
    writer.writeStartSegment("NAD");

    writer.writeElement("BY");

    writer.writeStartElement()
      .writeComponent("901494200")
      .writeComponent("")
      .writeComponent("31B")
      .endElement();

    writer.writeEndSegment();

    // Vendor ID and ID type
    messageSegmentCount++;
    writer.writeStartSegment("NAD");

    writer.writeElement("SU");

    writer.writeStartElement()
      .writeComponent("0142948")
      .writeComponent("")
      .writeComponent("31B")
      .endElement();

    writer.writeEndSegment();

    //
    messageSegmentCount++;
    writer.writeStartSegment("RFF");

    writer.writeStartElement()
      .writeComponent("API")
      .writeComponent("854674")
      .writeComponent("91")
      .endElement();

    writer.writeEndSegment();

    // Order currency - If FOLIO default currency and vendor currency are not the same, this may include info about both currencies,
    // with different qualifiers
    messageSegmentCount++;
    writer.writeStartSegment("CUX");

    writer.writeStartElement()
      .writeComponent("2")
      .writeComponent("GBP")
      .writeComponent("9")
      .endElement();

    writer.writeEndSegment();

    // Order lines
    int totalQuantity = 0;
    int totalNumberOfLineItems = 0;
    for (CompositePoLine poLine : compPO.getCompositePoLines()) {
      int segments = convertPOLine(poLine, writer, ++totalNumberOfLineItems);
      messageSegmentCount += segments;
      totalQuantity++; // supposed to be 'totalQuantity+=poLine.quantityOrdered;' but order line doesn't contain such field
    }

    // Summary (footer) separator - Indicates that all the line items of the PO are above, and next will be the summary of the PO
    messageSegmentCount++;
    writer.writeStartSegment("UNS");

    writer.writeElement("S");

    writer.writeEndSegment();

    // Order currency - If FOLIO default currency and vendor currency are not the same, this may include info about both currencies,
    // with different qualifiers
    messageSegmentCount++;
    writer.writeStartSegment("CNT");

    writer.writeStartElement()
      .writeComponent("1")
      .writeComponent(String.valueOf(totalQuantity))
      .endElement();

    writer.writeEndSegment();

    // Order currency - If FOLIO default currency and vendor currency are not the same, this may include info about both currencies,
    // with different qualifiers
    messageSegmentCount++;
    writer.writeStartSegment("CNT");

    writer.writeStartElement()
      .writeComponent("2")
      .writeComponent(String.valueOf(totalNumberOfLineItems))
      .endElement();

    writer.writeEndSegment();

    // Message trailer (aka footer) - Signals the end of this PO; may be followed by a UNH segment starting the next FOLIO PO in the
    // file
    messageSegmentCount++;
    writer.writeStartSegment("UNT")
      .writeElement(String.valueOf(messageSegmentCount))
      .writeElement(compPO.getPoNumber())
      .writeEndSegment();
  }

  private int convertPOLine(CompositePoLine poLine, EDIStreamWriter writer, int currentLineNumber) throws EDIStreamException {
    int messageSegmentCount = 0;

    // Product ID:ID type (EAN/UPC/barcode number)
    // May or may not be the same number as the POL line number, since the POLs may skip numbers, and the EDI order does not
    // Since ISBNs expanded to 13 digits, they are equivalent to UPC barcode numbers
    // Identifier qualifier not included
    messageSegmentCount++;
    writer.writeStartSegment("LIN");

    writer.writeElement(String.valueOf(currentLineNumber));
    writer.writeElement("");

    writer.writeStartElement()
      .writeComponent("9780393966510")
      .writeComponent("EN")
      .endElement();

    writer.writeEndSegment();

    // Product ID:ID type
    // ISBN-10 is no longer necessary
    // PIA includes whatever Product IDs are in the POL, not just an ISBN
    messageSegmentCount++;
    writer.writeStartSegment("PIA");

    writer.writeElement("5");

    String productId = "0393966518";
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
    writer.writeStartElement()
      .writeComponent(productId)
      .writeComponent("IB")
      .endElement();

    writer.writeEndSegment();

    // Author (Contributor)
    messageSegmentCount++;
    writer.writeStartSegment("IMD");

    writer.writeElement("L");

    writer.writeElement("009");

    String contributor = "Stiglitz, Joseph E.";
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
      .endElement();

    writer.writeEndSegment();

    // Title
    messageSegmentCount++;
    writer.writeStartSegment("IMD");

    writer.writeElement("L");

    writer.writeElement("050");

    writer.writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(poLine.getTitleOrPackage() != null ? poLine.getTitleOrPackage() : "Default Title")
      .endElement();

    writer.writeEndSegment();

    // Publisher
    messageSegmentCount++;
    writer.writeStartSegment("IMD");

    writer.writeElement("L");

    writer.writeElement("109");

    writer.writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(poLine.getPublisher() != null ? poLine.getPublisher() : "Default Publisher")
      .endElement();

    writer.writeEndSegment();

    // Place of publication - Not available from FOLIO POL
    messageSegmentCount++;
    writer.writeStartSegment("IMD");

    writer.writeElement("L");

    writer.writeElement("110");

    writer.writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent("New York ?")
      .endElement();

    writer.writeEndSegment();

    // Publication date
    messageSegmentCount++;
    writer.writeStartSegment("IMD");

    writer.writeElement("L");

    writer.writeElement("170");

    writer.writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent(poLine.getPublicationDate() != null ? poLine.getPublicationDate() : "1999")
      .endElement();

    writer.writeEndSegment();

    // Material type
    messageSegmentCount++;
    writer.writeStartSegment("IMD");

    writer.writeElement("L");

    writer.writeElement("180");

    writer.writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("")
      .writeComponent("Book")
      .endElement();

    writer.writeEndSegment();

    // Quantity ordered
    messageSegmentCount++;
    writer.writeStartSegment("QTY");

    writer.writeStartElement()
      .writeComponent("21")
      .writeComponent("1") // quantity value (always 1?)
      .endElement();

    writer.writeEndSegment();

    // Price - Several different kinds of qualifiers may be used; see documentation
    messageSegmentCount++;
    writer.writeStartSegment("PRI");

    String price = "49.99";
    if (poLine.getCost() != null && poLine.getCost()
      .getPoLineEstimatedPrice() != null) {
      price = String.valueOf(poLine.getCost()
        .getPoLineEstimatedPrice());
    }
    writer.writeStartElement()
      .writeComponent("AAB")
      .writeComponent(price)
      .endElement();

    writer.writeEndSegment();

    // FOLIO POL
    messageSegmentCount++;
    writer.writeStartSegment("RFF");

    writer.writeStartElement()
      .writeComponent("LI")
      .writeComponent(poLine.getPoLineNumber())
      .endElement();

    writer.writeEndSegment();

    // FOLIO fund code (Also expense class?)
    messageSegmentCount++;
    writer.writeStartSegment("RFF");

    writer.writeStartElement()
      .writeComponent("BFN")
      .writeComponent("LIBRARY")
      .endElement();

    writer.writeEndSegment();

    // Delivery location
    messageSegmentCount++;
    writer.writeStartSegment("LOC");

    writer.writeElement("20");

    writer.writeStartElement()
      .writeComponent("")
      .writeComponent("")
      .writeComponent("92")
      .endElement();

    writer.writeEndSegment();

    return messageSegmentCount;
  }
}
