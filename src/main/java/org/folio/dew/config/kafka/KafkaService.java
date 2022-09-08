package org.folio.dew.config.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.NewTopic;
import org.folio.spring.FolioExecutionContext;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;


@Component
@Log4j2
@RequiredArgsConstructor
public class KafkaService {

  public static final String EVENT_LISTENER_ID = "mod-data-export-events-listener";

  private final KafkaAdmin kafkaAdmin;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
  private final BeanFactory beanFactory;
  private final Environment springEnvironment;
  private final FolioExecutionContext folioExecutionContext;

  @Value("${env:folio}")
  private String environment;

  @RequiredArgsConstructor
  @AllArgsConstructor
  @Getter
  public enum Topic {
    JOB_UPDATE("data-export.job.update"),
    EXPORT_HISTORY_CREATE("Default", "edi-export-history.create");
    private String nameSpace;
    private final String topicName;
  }

  public void createKafkaTopics() {
    var tenantId = folioExecutionContext.getTenantId();
    List<NewTopic> newTopics = tenantSpecificTopics(tenantId);

    log.info("Creating topics for kafka [topics: {}]", newTopics);
    var configurableBeanFactory = (ConfigurableBeanFactory) beanFactory;
    newTopics.forEach(newTopic -> {
      var beanName = newTopic.name() + ".topic";
      if (!configurableBeanFactory.containsBean(beanName)) {
        configurableBeanFactory.registerSingleton(beanName, newTopic);
      }
    });
    kafkaAdmin.initialize();
  }

  /**
   * Restarts kafka event listeners in mod-data-export-spring application.
   */
  public void restartEventListeners() {
    log.info("Restarting kafka consumer to start listening created topics [id: {}]", EVENT_LISTENER_ID);
    var listenerContainer = kafkaListenerEndpointRegistry.getListenerContainer(EVENT_LISTENER_ID);
    Assert.notNull(listenerContainer, "Listener container not found");
    listenerContainer.stop();
    listenerContainer.start();
  }

  private List<NewTopic> tenantSpecificTopics(String tenant) {
    return Arrays.stream(Topic.values())
      .map(topic -> toKafkaTopic(tenant, topic))
      .collect(Collectors.toList());
  }

  private NewTopic toKafkaTopic(String tenant, Topic topic) {
    var envProperty = String.format("application.kafka.topic-configuration.%s.partitions", topic.getTopicName());
    var partitions = Integer.parseInt(springEnvironment.getProperty(envProperty, "50"));
    var tenantTopicName = getTenantTopicName(topic, tenant);
    return TopicBuilder.name(tenantTopicName).partitions(partitions).build();
  }

  /**
   * Returns topic name in the format - `{env}.{tenant}.topicName`
   *
   * @param topic initial topic name as {@link String}
   * @param tenantId tenant id as {@link String}
   * @return topic name as {@link String} object
   */
  private String getTenantTopicName(Topic topic, String tenantId) {
    String topicName = topic.getTopicName();
    String topicNameSpace = topic.getNameSpace();
    if (StringUtils.isNotEmpty(topicNameSpace)) {
      //Meet naming convension from folio-kafka-wrapper
      return String.format("%s.%s.%s.%s", environment, topicNameSpace, tenantId, topicName);
    }
    return String.format("%s.%s.%s", environment, tenantId, topicName);
  }

  public void send(Topic topic, String key, Object data) {
    log.info("Sending {}.", data);
    String tenant = folioExecutionContext.getTenantId();
    if (StringUtils.isBlank(tenant)) {
      throw new IllegalStateException("Can't send to Kafka because tenant is blank");
    }
    kafkaTemplate.send(getTenantTopicName(topic, tenant), key, data);
    log.info("Sent {}.", data);
  }
}
