package org.folio.dew.batch.acquisitions.edifact;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.folio.dew.domain.dto.CompositePoLine;
import org.folio.dew.domain.dto.CompositePurchaseOrder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class CompositePOConverter {
  private static final String DEFAULT_DATE = "20130620";
  private static final String RUSH_ORDER = "224";
  private static final String NOT_RUSH_ORDER = "220";

  private final CompositePOLineConverter compositePOLineConverter;

  public CompositePOConverter(CompositePOLineConverter compositePOLineConverter) {
    this.compositePOLineConverter = compositePOLineConverter;
  }

  public void convertPOtoEdifact(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    int messageSegmentCount = 0;

    messageSegmentCount++;
    writePOHeader(compPO, writer);

    String rushOrderQualifier = compPO.getCompositePoLines().stream().anyMatch(CompositePoLine::getRush) ? RUSH_ORDER : NOT_RUSH_ORDER;
    messageSegmentCount++;
    writePONumber(compPO, writer, rushOrderQualifier);

//    if (compPO.getDateOrdered() != null) {//TODO do we really want to use default date?
    messageSegmentCount++;
    writeOrderDate(compPO, writer);
//    }

    messageSegmentCount++;
    writeLibrary(compPO, writer);

    messageSegmentCount++;
    writeVendor(compPO, writer);

    if (!compPO.getCompositePoLines().isEmpty()
        && compPO.getCompositePoLines().get(0).getVendorDetail() != null
        && compPO.getCompositePoLines().get(0).getVendorDetail().getVendorAccount() != null){
      messageSegmentCount++;
      writeAccountNumber(compPO.getCompositePoLines().get(0).getVendorDetail().getVendorAccount(), writer);
    }

    messageSegmentCount++;
    writeCurrency(compPO, writer);

    // Order lines
    int totalQuantity = 0;
    int totalNumberOfLineItems = 0;
    for (CompositePoLine poLine : compPO.getCompositePoLines()) {
      int quantityOrdered = getPoLineQuantityOrdered(poLine);
      int segments = compositePOLineConverter.convertPOLine(poLine, writer, ++totalNumberOfLineItems, quantityOrdered);
      messageSegmentCount += segments;
      totalQuantity += quantityOrdered;
    }

    messageSegmentCount++;
    writeEndPoLines(compPO, writer);

    messageSegmentCount++;
    writePoLinesQuantity(compPO, writer, totalQuantity);

    messageSegmentCount++;
    writeNumberLineItems(compPO, writer, totalNumberOfLineItems);

    messageSegmentCount++;
    writePOFooter(compPO, writer, messageSegmentCount);
  }

  // Order header = Start of order; EDIFACT message type - There would be a new UNH for each FOLIO PO in the file
  private void writePOHeader(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("UNH")
      .writeElement("1001")
      .writeStartElement()
      .writeComponent("ORDERS")
      .writeComponent("D")
      .writeComponent("96A")
      .writeComponent("UN")
      .writeComponent("EAN008")
      .endElement()
      .writeEndSegment();
  }

  // FOLIO PO
  private void writePONumber(CompositePurchaseOrder compPO, EDIStreamWriter writer, String rushOrderQualifier) throws EDIStreamException {
    writer.writeStartSegment("BGM")
      .writeElement(rushOrderQualifier)
      .writeElement(compPO.getPoNumber())
      .writeElement("9")
      .writeEndSegment();
  }

  // Order date:Date format
  private void writeOrderDate(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
    writer.writeStartSegment("DTM")
      .writeStartElement()
      .writeComponent("137")
      .writeComponent(compPO.getDateOrdered() != null ? dateFormat.format(compPO.getDateOrdered()) : DEFAULT_DATE)
      .writeComponent("102")
      .endElement()
      .writeEndSegment();
  }

  // Library ID and ID type
  private void writeLibrary(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("NAD")
      .writeElement("BY")
      .writeStartElement()
      .writeComponent("901494200")
      .writeComponent("")
      .writeComponent("31B")
      .endElement()
      .writeEndSegment();
  }

  // Vendor ID and ID type
  private void writeVendor(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("NAD")
      .writeElement("SU")
      .writeStartElement()
      .writeComponent("0142948")
      .writeComponent("")
      .writeComponent("31B")
      .endElement()
      .writeEndSegment();
  }

  private void writeAccountNumber(String accountNumber, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("API")
      .writeComponent(accountNumber)
      .writeComponent("91")
      .endElement()
      .writeEndSegment();
  }

  // Order currency - If FOLIO default currency and vendor currency are not the same, this may include info about both currencies,
  // with different qualifiers
  private void writeCurrency(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("CUX")
      .writeStartElement()
      .writeComponent("2")
      .writeComponent("GBP")
      .writeComponent("9")
      .endElement()
      .writeEndSegment();
  }

  // Summary (footer) separator - Indicates that all the line items of the PO are above, and next will be the summary of the PO
  private void writeEndPoLines(CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("UNS")
      .writeElement("S")
      .writeEndSegment();
  }

  // Total quantity
  // Calculated by adding all the QTY in the individual line items
  private void writePoLinesQuantity(CompositePurchaseOrder compPO, EDIStreamWriter writer, int totalQuantity) throws EDIStreamException {
    writer.writeStartSegment("CNT")
      .writeStartElement()
      .writeComponent("1")
      .writeComponent(String.valueOf(totalQuantity))
      .endElement()
      .writeEndSegment();
  }

  // Total number of line items
  // 	If any line has QTY >1, then CNT+1 ≠ CNT+2
  private void writeNumberLineItems(CompositePurchaseOrder compPO, EDIStreamWriter writer, int totalNumberOfLineItems) throws EDIStreamException {
    writer.writeStartSegment("CNT")
      .writeStartElement()
      .writeComponent("2")
      .writeComponent(String.valueOf(totalNumberOfLineItems))
      .endElement()
      .writeEndSegment();
  }

  // Message trailer (aka footer) - Signals the end of this PO; may be followed by a UNH segment starting the next FOLIO PO in the
  // file
  private void writePOFooter(CompositePurchaseOrder compPO, EDIStreamWriter writer, int messageSegmentCount) throws EDIStreamException {
    writer.writeStartSegment("UNT")
      .writeElement(String.valueOf(messageSegmentCount))
      .writeElement(compPO.getPoNumber())
      .writeEndSegment();
  }

  private int getPoLineQuantityOrdered(CompositePoLine poLine) {
    int quantity;

    int quantityPhysical = 0;
    if (poLine.getPhysical() != null && poLine.getCost().getQuantityPhysical() != null) {
      quantityPhysical = poLine.getCost().getQuantityPhysical();
    }
    int quantityElectronic = 0;
    if (poLine.getEresource() != null && poLine.getCost().getQuantityElectronic() != null) {
      quantityElectronic = poLine.getCost().getQuantityElectronic();
    }

    if (quantityPhysical != 0 && quantityElectronic != 0) {
      quantity = Math.max(quantityPhysical, quantityElectronic);
    } else {
      quantity = quantityPhysical + quantityElectronic;
    }

    return quantity;
  }
}
