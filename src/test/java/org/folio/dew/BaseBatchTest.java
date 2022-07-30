package org.folio.dew;

import org.folio.dew.batch.ExportJobManagerSync;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.repository.InMemoryAcknowledgementRepository;
import org.folio.dew.repository.MinIOObjectStorageRepository;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionScopeExecutionContextManager;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.SocketUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import lombok.SneakyThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@AutoConfigureMockMvc
@EmbeddedKafka(topics = { "diku.data-export.job.command" })
@EnableKafka
@EnableBatchProcessing
public abstract class BaseBatchTest {
  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";

  public static final int WIRE_MOCK_PORT = SocketUtils.findAvailableTcpPort();
  public static WireMockServer wireMockServer;

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;
  @Autowired
  protected JobLauncher jobLauncher;
  @Autowired
  protected JobRepository jobRepository;
  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  protected MinIOObjectStorageRepository minIOObjectStorageRepository;
  @SpyBean
  protected JobCommandsReceiverService jobCommandsReceiverService;
  @Autowired
  protected InMemoryAcknowledgementRepository repository;
  @MockBean
  @Qualifier("exportJobManager")
  protected ExportJobManager exportJobManager;
  @MockBean
  @Qualifier("exportJobManagerSync")
  protected ExportJobManagerSync exportJobManagerSync;
  @MockBean
  protected Acknowledgment acknowledgment;

  @Value("${spring.application.name}")
  protected String springApplicationName;

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();

    setUpTenant(mockMvc);
  }

  @SneakyThrows
  protected static void setUpTenant(MockMvc mockMvc) {
    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().moduleTo("mod-data-export-worker")))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    httpHeaders.add(XOkapiHeaders.URL, wireMockServer.baseUrl());
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    return httpHeaders;
  }

  @BeforeEach
  void setUp() {
    Map<String, Collection<String>> okapiHeaders = new LinkedHashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT));
    okapiHeaders.put(XOkapiHeaders.TOKEN, List.of(TOKEN));
    okapiHeaders.put(XOkapiHeaders.URL, List.of(wireMockServer.baseUrl()));
    okapiHeaders.put(XOkapiHeaders.USER_ID, List.of(UUID.randomUUID().toString()));
    var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, okapiHeaders);
    FolioExecutionScopeExecutionContextManager.beginFolioExecutionContext(defaultFolioExecutionContext);

    minIOObjectStorageRepository.createBucketIfNotExists();
  }

  @AfterEach
  void eachTearDown() {
    FolioExecutionScopeExecutionContextManager.endFolioExecutionContext();
  }

  protected JobLauncherTestUtils createTestLauncher(Job job) {
    JobLauncherTestUtils testLauncher = new JobLauncherTestUtils();
    testLauncher.setJob(job);
    testLauncher.setJobLauncher(jobLauncher);
    testLauncher.setJobRepository(jobRepository);
    return testLauncher;
  }

  protected FileSystemResource actualFileOutput(String spec) throws IOException {
    InputStream inputStream = new URL(spec).openStream();
    final Path actualResult = Files.createTempFile("temp", ".tmp");
    Files.copy(inputStream, actualResult, StandardCopyOption.REPLACE_EXISTING);
    return new FileSystemResource(actualResult);
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }

}
