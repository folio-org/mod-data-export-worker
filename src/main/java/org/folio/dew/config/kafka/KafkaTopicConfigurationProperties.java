package org.folio.dew.config.kafka;

import static java.util.Collections.emptyMap;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "application.kafka.topic-configuration")
public class KafkaTopicConfigurationProperties {

  /**
   * Map with topic configuration, key - topic name, value - corresponding configuration.
   */
  private Map<String, KafkaTopicConfiguration> topics = emptyMap();

  @Data
  public static class KafkaTopicConfiguration {

    /**
     * Number of partitions.
     */
    private int partitions;

    /**
     * Topic namespace.
     */
    private String nameSpace;
  }
}
