package org.folio.dew.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaConfiguration implements DefaultKafkaConsumerFactoryCustomizer {

  private final ObjectMapper objectMapper;

  @Override
  public void customize(DefaultKafkaConsumerFactory<?, ?> consumerFactory) {
    consumerFactory.setValueDeserializer(((JsonDeserializer) new JsonDeserializer<>(objectMapper)).trustedPackages("*"));
  }

}
