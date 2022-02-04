package org.folio.dew.batch.acquisitions.edifact;

import io.xlate.edi.stream.EDIOutputFactory;
import io.xlate.edi.stream.EDIStreamException;
import io.xlate.edi.stream.EDIStreamWriter;
import org.folio.dew.domain.dto.CompositePurchaseOrder;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MappingOrdersToEdifact {
  private final CompositePOConverter compositePOConverter;

  public MappingOrdersToEdifact(CompositePOConverter compositePOConverter) {
    this.compositePOConverter = compositePOConverter;
  }

  public String convertOrdersToEdifact(List<CompositePurchaseOrder> compPOs) throws EDIStreamException {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();

    EDIOutputFactory factory = EDIOutputFactory.newFactory();
    factory.setProperty(EDIOutputFactory.PRETTY_PRINT, true);

    EDIStreamWriter writer = factory.createEDIStreamWriter(stream);

    // Count of messages (one message per purchase order)
    int messageCount = 0;
    writer.startInterchange();
    writeStartFile(writer);

    String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyyhhmm"));
    String dateOfPreparation = dateTime.substring(0, 6);
    String timeOfPreparation = dateTime.substring(6);
    writeInterchangeHeader(writer, dateOfPreparation, timeOfPreparation);

    // Purchase orders
    for (CompositePurchaseOrder compPO : compPOs) {
      compositePOConverter.convertPOtoEdifact(compPO, writer);
      messageCount++;
    }

    writeInterchangeFooter(writer, messageCount);
    writer.endInterchange();
    writer.close();

    return stream.toString();
  }

  public byte[] convertOrdersToEdifactArray(List<CompositePurchaseOrder> compPOs) throws EDIStreamException {
    return convertOrdersToEdifact(compPOs).getBytes(StandardCharsets.UTF_8);
  }

  // Start of file - Can contain multiple order messages
  private void writeStartFile(EDIStreamWriter writer) throws EDIStreamException {
    writer.writeStartSegment("UNA")
      .writeEndSegment();
  }

  // Interchange header (Library ID:ID type; Vendor ID:ID type)
  private void writeInterchangeHeader(EDIStreamWriter writer, String dateOfPreparation, String timeOfPreparation) throws EDIStreamException {
    writer.writeStartSegment("UNB")
      .writeStartElement()
      .writeComponent("UNOC")
      .writeComponent("1")
      .endElement();

    writer.writeStartElement()
      .writeComponent("901494200")
      .writeComponent("31B")
      .endElement()
      .writeStartElement()
      .writeComponent("12345")
      .writeComponent("31B")
      .endElement()
      .writeStartElement()
      .writeComponent(dateOfPreparation)
      .writeComponent(timeOfPreparation)
      .endElement();

    writer.writeElement("1001")
      .writeEndSegment();
  }

  // Interchange trailer (aka end of file)
  private void writeInterchangeFooter(EDIStreamWriter writer, int messageCount) throws EDIStreamException {
    writer.writeStartSegment("UNZ")
      .writeElement(String.valueOf(messageCount))
      .writeElement("1001")
      .writeEndSegment();
  }
}
