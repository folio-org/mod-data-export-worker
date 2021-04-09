package org.folio.dew.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.des.config.KafkaConfiguration;
import org.folio.spring.FolioExecutionContext;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@org.springframework.context.annotation.Configuration
public class Configuration {

  @Bean
  public KafkaConfiguration kafkaConfiguration(KafkaTemplate<String, Object> kafkaTemplate,
      FolioExecutionContext folioExecutionContext, ObjectMapper objectMapper) {
    return new KafkaConfiguration(kafkaTemplate, folioExecutionContext, objectMapper);
  }

}
