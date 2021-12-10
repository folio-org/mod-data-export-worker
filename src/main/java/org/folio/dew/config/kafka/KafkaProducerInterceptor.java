package org.folio.dew.config.kafka;

import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.folio.spring.FolioExecutionContext;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

@Log4j2
public class KafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

  private FolioExecutionContext folioExecutionContext;

  @Override
  public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> producerRecord) {
    folioExecutionContext.getOkapiHeaders().entrySet().stream()
        .map(this::toRecordHeader)
        .forEach(recordHeader -> producerRecord.headers().add(recordHeader));
    return producerRecord;
  }

  @Override
  public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
    // Nothing to do
  }

  @Override
  public void close() {
    // Nothing to do
  }

  @Override
  public void configure(Map<String, ?> configs) {
    this.folioExecutionContext = (FolioExecutionContext) configs.get("folioExecutionContext");
  }

  private RecordHeader toRecordHeader(Entry<String, Collection<String>> entry) {
    byte[] value =
        entry.getValue()
          .stream()
          .findFirst()
          .orElse("")
          .getBytes(StandardCharsets.UTF_8);
    return new RecordHeader(entry.getKey(), value);
  }
}
