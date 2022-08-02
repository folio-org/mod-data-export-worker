package org.folio.dew.config.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;

@Log4j2
@Component
@RequiredArgsConstructor
public class KafkaRecordInterceptor<V> implements RecordInterceptor<String, V> {

  private final FolioModuleMetadata folioModuleMetadata;

  @Override
  public ConsumerRecord<String, V> intercept(ConsumerRecord<String, V> consumerRecord) {
    Map<String, Collection<String>> okapiHeaders = headersToMap(consumerRecord.headers());

    var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);
    log.debug("FOLIO context initialized.");

    return consumerRecord;
  }

  @Override
  public void afterRecord(ConsumerRecord<String, V> record, Consumer<String, V> consumer) {
  }

  private Map<String, Collection<String>> headersToMap(Headers header) {
    Iterator<Header> headerIterator = header.iterator();
    Map<String, Collection<String>> okapiHeaders = new HashMap<>();
    while (headerIterator.hasNext()) {
      var next = headerIterator.next();
      if (next.key().startsWith(XOkapiHeaders.OKAPI_HEADERS_PREFIX)) {
        var value = List.of(new String(next.value(), StandardCharsets.UTF_8));
        okapiHeaders.put(next.key(), value);
      }
    }
    return okapiHeaders;
  }
}
