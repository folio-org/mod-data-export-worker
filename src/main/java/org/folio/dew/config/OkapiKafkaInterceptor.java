package org.folio.dew.config;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@NoArgsConstructor
public class OkapiKafkaInterceptor implements ConsumerInterceptor {

  private FolioModuleMetadata folioModuleMetadata;

  @Autowired
  public void setFolioModuleMetadata(FolioModuleMetadata folioModuleMetadata) {
    this.folioModuleMetadata = folioModuleMetadata;
  }

  @Override
  public ConsumerRecords onConsume(ConsumerRecords records) {
    Iterator<ConsumerRecord> iterator = records.iterator();
    if (iterator.hasNext()) {
      ConsumerRecord record = iterator.next();
      Map<String, Collection<String>> okapiHeaders = headersToMap(record.headers());

      var defaultFolioExecutionContext =
          new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
      FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(
          defaultFolioExecutionContext);
    }
    return records;
  }

  private Map<String, Collection<String>> headersToMap(Headers header) {
    Iterator<Header> headerIterator = header.iterator();
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    while (headerIterator.hasNext()) {
      Header next = headerIterator.next();
      if (next.key().startsWith("x-okapi-")) {
        var value = List.of(new String(next.value(), StandardCharsets.UTF_8));
        okapiHeaders.put(next.key(), value);
      }
    }
    return okapiHeaders;
  }

  @Override
  public void close() {}

  @Override
  public void onCommit(Map offsets) {
    FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
  }

  @Override
  public void configure(Map<String, ?> configs) {}
}
