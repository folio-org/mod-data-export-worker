package org.folio.dew.config.kafka;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.folio.de.entity.JobCommand;
import org.folio.de.entity.JobCommandType;
import org.folio.dew.BaseBatchTest;
import org.folio.dew.batch.acquisitions.services.FTPStorageService;
import org.folio.dew.domain.dto.ExportType;
import org.folio.dew.repository.JobCommandRepository;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.kafka.filtering.entitlement.TenantEntitlementClient;
import org.folio.spring.kafka.filtering.filter.EnabledTenantMessageFilterStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestPropertySource(properties = {
  "folio.kafka.tenant-filter.enabled=true",
  "folio.kafka.tenant-filter.tenant-disabled-strategy=skip",
  "folio.kafka.tenant-filter.all-tenants-disabled-strategy=skip"
})
class KafkaTenantFilterEnabledTest extends BaseBatchTest {

  private static final String JOB_COMMAND_TOPIC = "diku.data-export.job.command";
  private static final String NOT_ENTITLED_TENANT = "not_entitled";
  private static final String MESSAGE_KEY = "same-key-keeps-messages-in-one-partition";
  private static final long WAIT_MS = 30_000;

  @MockitoBean
  private TenantEntitlementClient tenantEntitlementClient;
  @MockitoBean
  private JobCommandRepository jobCommandRepository;
  @MockitoBean
  private RemoteFilesStorage remoteFilesStorage;
  @MockitoBean
  private FTPStorageService ftpStorageService;

  @Autowired
  @Qualifier("tenantAwareMessageFilter")
  private RecordFilterStrategy<?, ?> tenantAwareMessageFilter;
  @Autowired
  private ConcurrentKafkaListenerContainerFactory<String, ?> kafkaListenerContainerFactory;
  @Autowired
  @Qualifier("consumerFactory")
  private ConsumerFactory<String, ?> consumerFactory;
  @Autowired
  private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;
  @Autowired
  private EmbeddedKafkaBroker embeddedKafkaBroker;

  @BeforeEach
  void setUpListener() {
    when(tenantEntitlementClient.lookupTenantsByModuleId(anyString())).thenReturn(Set.of(NON_CONSORTIUM_TENANT));

    var container = kafkaListenerEndpointRegistry.getListenerContainer(KafkaService.EVENT_LISTENER_ID);
    assertNotNull(container);
    // the listener subscribes by pattern, which matches only the job command topic of the embedded broker
    ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
  }

  @Test
  @DisplayName("Context starts with tenant filter enabled and uses the module consumer factory")
  void contextStartsWithTenantFilterEnabled() {
    assertInstanceOf(EnabledTenantMessageFilterStrategy.class, tenantAwareMessageFilter);
    assertNotNull(kafkaListenerContainerFactory);
    assertSame(consumerFactory, kafkaListenerContainerFactory.getConsumerFactory());
  }

  @Test
  @DisplayName("Start CirculationLog job for entitled tenant")
  void startJobForEntitledTenant() throws Exception {
    var id = UUID.randomUUID();

    sendJobCommand(NON_CONSORTIUM_TENANT, createStartCirculationLogJobRequest(id));

    verify(jobCommandsReceiverService, timeout(WAIT_MS).times(1))
      .receiveStartJobCommand(argThat(command -> id.equals(command.getId())), anyMap());
    verify(exportJobManagerSync, timeout(WAIT_MS).times(1)).launchJob(any());
  }

  @Test
  @DisplayName("Skip job command for not entitled tenant")
  void skipJobCommandForNotEntitledTenant() throws Exception {
    var skippedId = UUID.randomUUID();
    var processedId = UUID.randomUUID();

    // both messages use the same key, so they are consumed in the order they were sent
    sendJobCommand(NOT_ENTITLED_TENANT, createStartCirculationLogJobRequest(skippedId));
    sendJobCommand(NON_CONSORTIUM_TENANT, createStartCirculationLogJobRequest(processedId));

    verify(jobCommandsReceiverService, timeout(WAIT_MS).times(1))
      .receiveStartJobCommand(argThat(command -> processedId.equals(command.getId())), anyMap());
    verify(jobCommandsReceiverService, never())
      .receiveStartJobCommand(argThat(command -> skippedId.equals(command.getId())), anyMap());
    verify(exportJobManagerSync, times(1)).launchJob(any());
  }

  private static JobCommand createStartCirculationLogJobRequest(UUID id) {
    JobCommand jobCommand = new JobCommand();
    jobCommand.setType(JobCommandType.START);
    jobCommand.setId(id);
    jobCommand.setName(ExportType.CIRCULATION_LOG.toString());
    jobCommand.setDescription("Start job test desc");
    jobCommand.setExportType(ExportType.CIRCULATION_LOG);

    var paramBuilder = new JobParametersBuilder();
    paramBuilder.addString("query", "");
    jobCommand.setJobParameters(paramBuilder.toJobParameters());
    return jobCommand;
  }

  private static Map<String, String> okapiHeaders(String tenant) {
    var headers = new HashMap<String, String>();
    headers.put(XOkapiHeaders.TENANT, tenant);
    headers.put(XOkapiHeaders.TOKEN, TOKEN);
    headers.put(XOkapiHeaders.URL, wireMockServer.baseUrl());
    headers.put(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());
    return headers;
  }

  private void sendJobCommand(String tenant, JobCommand jobCommand) throws Exception {
    var producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
    try (var producer = new KafkaProducer<>(producerProps, new StringSerializer(), new StringSerializer())) {
      var message = new ProducerRecord<>(JOB_COMMAND_TOPIC, MESSAGE_KEY, asJsonString(jobCommand));
      okapiHeaders(tenant).forEach((name, value) -> message.headers().add(name, value.getBytes(UTF_8)));
      message.headers().add("__TypeId__", JobCommand.class.getName().getBytes(UTF_8));
      producer.send(message).get();
    }
  }
}
