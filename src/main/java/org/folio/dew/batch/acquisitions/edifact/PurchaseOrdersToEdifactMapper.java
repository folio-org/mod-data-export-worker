package org.folio.dew.batch.acquisitions.edifact;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.folio.dew.domain.dto.CompositePurchaseOrder;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.folio.dew.domain.dto.VendorEdiOrdersExportConfig;
import org.folio.dew.domain.dto.acquisitions.edifact.EdiFileConfig;

public class PurchaseOrdersToEdifactMapper {
  private final CompositePOConverter compositePOConverter;

  public PurchaseOrdersToEdifactMapper(CompositePOConverter compositePOConverter) {
    this.compositePOConverter = compositePOConverter;
  }

  public String convertOrdersToEdifact(List<CompositePurchaseOrder> compPOs, VendorEdiOrdersExportConfig ediExportConfig, String jobName) throws EDIStreamException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    EDIOutputFactory factory = EDIOutputFactory.newFactory();
    factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);

    try (EDIStreamWriter writer = factory.createEDIStreamWriter(stream)) {
      // Count of messages (one message per purchase order)
      int messageCount = 0;
      writer.startInterchange();
      writeStartFile(writer);

      EdiFileConfig ediFileConfig = new EdiFileConfig();
      ediFileConfig.setFileId(StringUtils.right(jobName, 14));
      ediFileConfig.setLibEdiCode(ediExportConfig.getEdiConfig().getLibEdiCode());
      ediFileConfig.setLibEdiType(ediExportConfig.getEdiConfig().getLibEdiType().getValue().substring(0, 3));
      ediFileConfig.setVendorEdiCode(ediExportConfig.getEdiConfig().getVendorEdiCode());
      ediFileConfig.setVendorEdiType(ediExportConfig.getEdiConfig().getVendorEdiType().getValue().substring(0, 3));

      writeInterchangeHeader(writer, ediFileConfig);

      // Purchase orders
      for (CompositePurchaseOrder compPO : compPOs) {
        compositePOConverter.convertPOtoEdifact(writer, compPO, ediFileConfig);
        messageCount++;
      }

      writeInterchangeFooter(writer, ediFileConfig.getFileId(), messageCount);
      writer.endInterchange();
      return stream.toString();
    }
  }

  public byte[] convertOrdersToEdifactArray(List<CompositePurchaseOrder> compPOs, VendorEdiOrdersExportConfig ediExportConfig, String jobName) throws EDIStreamException {
    return convertOrdersToEdifact(compPOs, ediExportConfig, jobName).getBytes(StandardCharsets.UTF_8);
  }

  // Start of file - Can contain multiple order messages
  private void writeStartFile(EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("UNA")
      .writeEndSegment();
  }

  private void writeInterchangeHeader(EDIStreamWriter writer, EdiFileConfig ediFileConfig) throws EDIStreamException {
    String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyhhmm"));
    String dateOfPreparation = dateTime.substring(0, 6);
    String timeOfPreparation = dateTime.substring(6);
    writeInterchangeHeader(writer, ediFileConfig, dateOfPreparation, timeOfPreparation);
  }

  // Interchange header (Library ID:ID type; Vendor ID:ID type)
  private void writeInterchangeHeader(EDIStreamWriter writer, EdiFileConfig ediFileConfig,
                                      String dateOfPreparation, String timeOfPreparation) throws EDIStreamException {
    writer.writeStartSegment("UNB")
      .writeStartElement()
      .writeComponent("UNOC")
      .writeComponent("3")
      .endElement();

    writer.writeStartElement()
      .writeComponent(ediFileConfig.getLibEdiCode())
      .writeComponent(ediFileConfig.getLibEdiType())
      .endElement()
      .writeStartElement()
      .writeComponent(ediFileConfig.getVendorEdiCode())
      .writeComponent(ediFileConfig.getVendorEdiType())
      .endElement()
      .writeStartElement()
      .writeComponent(dateOfPreparation)
      .writeComponent(timeOfPreparation)
      .endElement();

    writer.writeElement(ediFileConfig.getFileId())
      .writeEndSegment();
  }

  // Interchange trailer (aka end of file)
  private void writeInterchangeFooter(EDIStreamWriter writer, String fileId, int messageCount) throws EDIStreamException {
    writer.writeStartSegment("UNZ")
      .writeElement(String.valueOf(messageCount))
      .writeElement(fileId)
      .writeEndSegment();
  }
}
