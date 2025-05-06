package org.folio.dew.batch.acquisitions.mapper.converter;

import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.batch.acquisitions.services.ConfigurationService;
import org.folio.dew.domain.dto.CompositePurchaseOrder;
import org.folio.dew.domain.dto.Piece;
import org.folio.dew.domain.dto.PoLine;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.EdiFileConfig;
import org.folio.dew.error.NotFoundException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class CompOrderEdiConverter {
  private static final String RUSH_ORDER = "224";
  private static final String NOT_RUSH_ORDER = "220";

  private final PoLineEdiConverter poLineEdiConverter;
  private final ConfigurationService configurationService;

  public CompOrderEdiConverter(PoLineEdiConverter poLineEdiConverter, ConfigurationService configurationService) {
    this.poLineEdiConverter = poLineEdiConverter;
    this.configurationService = configurationService;
  }

  public void convertPOtoEdifact(EDIStreamWriter writer, CompositePurchaseOrder compPO,
                                 Map<String, List<Piece>> poLineToPieces, EdiFileConfig ediFileConfig,
                                 VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType) throws EDIStreamException {
    int messageSegmentCount = 0;

    messageSegmentCount++;
    writePOHeader(integrationType, compPO, writer);

    String rushOrderQualifier = compPO.getPoLines().stream().filter(line -> line.getRush() != null).anyMatch(PoLine::getRush) ? RUSH_ORDER : NOT_RUSH_ORDER;
    messageSegmentCount++;
    writePONumber(compPO, writer, rushOrderQualifier);

    messageSegmentCount++;
    writeOrderDate(compPO, writer);

    String shipToAddress = configurationService.getAddressConfig(compPO.getShipTo());
    messageSegmentCount++;
    writeLibrary(ediFileConfig, shipToAddress, writer);

    messageSegmentCount++;
    writeVendor(ediFileConfig, writer);

    if (compPO.getPoLines().isEmpty()) {
      String errMsg = String.format("poLines is not found for CompositeOrder '%s'", compPO.getId());
      throw new NotFoundException(errMsg);
    }

    var comPoLine = compPO.getPoLines().getFirst();
    if (comPoLine.getVendorDetail() != null && StringUtils.isNotBlank(comPoLine.getVendorDetail().getVendorAccount())){
      messageSegmentCount++;
      writeAccountNumber(comPoLine.getVendorDetail().getVendorAccount(), writer);
    }

    messageSegmentCount++;
    String currency = comPoLine.getCost().getCurrency();
    if (integrationType == VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING) {
      writeCurrency(currency, writer);
    }

    // Order lines
    int totalQuantity = 0;
    int totalNumberOfLineItems = 0;
    for (PoLine poLine : compPO.getPoLines()) {
      int quantityOrdered = getPoLineQuantityOrdered(poLine);
      var pieces = poLineToPieces.getOrDefault(poLine.getId(), List.of());
      int segments = poLineEdiConverter.convertPOLine(integrationType, compPO, poLine, pieces, writer, ++totalNumberOfLineItems, quantityOrdered);
      messageSegmentCount += segments;
      totalQuantity += quantityOrdered;
    }

    messageSegmentCount++;
    writeEndPoLines(writer);

    messageSegmentCount++;
    writePoLinesQuantity(writer, totalQuantity);

    messageSegmentCount++;
    writeNumberLineItems(writer, totalNumberOfLineItems);

    messageSegmentCount++;
    writePOFooter(compPO, writer, messageSegmentCount);
  }

  // Order header = Start of order; EDIFACT message type - There would be a new UNH for each FOLIO PO in the file
  private void writePOHeader(VendorEdiOrdersExportConfig.IntegrationTypeEnum integrationType,
                             CompositePurchaseOrder compPO, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("UNH")
      .writeElement(compPO.getPoNumber())
      .writeStartElement()
      .writeComponent(integrationType == VendorEdiOrdersExportConfig.IntegrationTypeEnum.ORDERING ? "ORDERS" : "OSTENQ")
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
      .writeComponent(compPO.getDateOrdered() != null ? dateFormat.format(compPO.getDateOrdered()) : "")
      .writeComponent("102")
      .endElement()
      .writeEndSegment();
  }

  // Library ID and ID type
  private void writeLibrary(EdiFileConfig ediFileConfig, String shipToAddress, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("NAD")
      .writeElement("BY")
      .writeStartElement()
      .writeComponent(ediFileConfig.getLibEdiCode())
      .writeComponent("")
      .writeComponent(ediFileConfig.getLibEdiType())
      .endElement()
      .writeStartElement()
      .writeComponent("")
      .endElement()
      .writeStartElement()
      .writeComponent(shipToAddress)
      .endElement()
      .writeEndSegment();
  }

  // Vendor ID and ID type
  private void writeVendor(EdiFileConfig ediFileConfig, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("NAD")
      .writeElement("SU")
      .writeStartElement()
      .writeComponent(ediFileConfig.getVendorEdiCode())
      .writeComponent("")
      .writeComponent(ediFileConfig.getVendorEdiType())
      .endElement()
      .writeEndSegment();
  }

  private void writeAccountNumber(String accountNumber, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("RFF")
      .writeStartElement()
      .writeComponent("API")
      .writeComponent(accountNumber)
      .endElement()
      .writeEndSegment();
  }

  // Order currency - If FOLIO default currency and vendor currency are not the same, this may include info about both currencies,
  // with different qualifiers
  private void writeCurrency(String currency, EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("CUX")
      .writeStartElement()
      .writeComponent("2")
      .writeComponent(currency)
      .writeComponent("9")
      .endElement()
      .writeEndSegment();
  }

  // Summary (footer) separator - Indicates that all the line items of the PO are above, and next will be the summary of the PO
  private void writeEndPoLines(EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("UNS")
      .writeElement("S")
      .writeEndSegment();
  }

  // Total quantity
  // Calculated by adding all the QTY in the individual line items
  private void writePoLinesQuantity(EDIStreamWriter writer, int totalQuantity) throws EDIStreamException {
    writer.writeStartSegment("CNT")
      .writeStartElement()
      .writeComponent("1")
      .writeComponent(String.valueOf(totalQuantity))
      .endElement()
      .writeEndSegment();
  }

  // Total number of line items
  // 	If any line has QTY >1, then CNT+1 ≠ CNT+2
  private void writeNumberLineItems(EDIStreamWriter writer, int totalNumberOfLineItems) throws EDIStreamException {
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

  private int getPoLineQuantityOrdered(PoLine poLine) {
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
