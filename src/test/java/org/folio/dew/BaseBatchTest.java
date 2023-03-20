package org.folio.dew;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.folio.dew.batch.ExportJobManager;
import org.folio.dew.batch.ExportJobManagerSync;
import org.folio.dew.repository.RemoteFilesStorage;
import org.folio.dew.service.JobCommandsReceiverService;
import org.folio.spring.DefaultFolioExecutionContext;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
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
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = BaseBatchTest.DockerPostgreDataSourceInitializer.class)
@Testcontainers
@AutoConfigureMockMvc
@EmbeddedKafka(topics = { "diku.data-export.job.command" })
@EnableBatchProcessing
@EnableAutoConfiguration
public abstract class BaseBatchTest {
  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String TENANT = "diku";

  public static final int WIRE_MOCK_PORT = TestSocketUtils.findAvailableTcpPort();
  public static WireMockServer wireMockServer;
  public static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:13");

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
  protected RemoteFilesStorage remoteFilesStorage;
  @SpyBean
  protected JobCommandsReceiverService jobCommandsReceiverService;
  @MockBean
  @Qualifier("exportJobManager")
  protected ExportJobManager exportJobManager;
  @MockBean
  @Qualifier("exportJobManagerSync")
  protected ExportJobManagerSync exportJobManagerSync;
  @Value("${spring.application.name}")
  protected String springApplicationName;

  static {
    postgreDBContainer.start();
  }

  protected Map<String, Object> okapiHeaders = new HashMap<>();
  protected FolioExecutionContextSetter folioExecutionContextSetter;

  public static class DockerPostgreDataSourceInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
      TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,
        "spring.datasource.url=" + postgreDBContainer.getJdbcUrl(),
        "spring.datasource.username=" + postgreDBContainer.getUsername(),
        "spring.datasource.password=" + postgreDBContainer.getPassword());
    }
  }

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
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .registerModule(new JavaTimeModule());

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
    okapiHeaders = new LinkedHashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, TENANT);
    okapiHeaders.put(XOkapiHeaders.TOKEN, TOKEN);
    okapiHeaders.put(XOkapiHeaders.URL, wireMockServer.baseUrl());
    okapiHeaders.put(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    var localHeaders =
      okapiHeaders.entrySet()
        .stream()
        .filter(e -> e.getKey().startsWith(XOkapiHeaders.OKAPI_HEADERS_PREFIX))
        .collect(Collectors.toMap(Map.Entry::getKey, e -> (Collection<String>)List.of(String.valueOf(e.getValue()))));

    var defaultFolioExecutionContext = new DefaultFolioExecutionContext(folioModuleMetadata, localHeaders);
    folioExecutionContextSetter = new FolioExecutionContextSetter(defaultFolioExecutionContext);

    remoteFilesStorage.createBucketIfNotExists();
  }

  @AfterEach
  void eachTearDown() {
    folioExecutionContextSetter.close();
  }

  protected JobLauncherTestUtils createTestLauncher(Job job) {
    JobLauncherTestUtils testLauncher = new JobLauncherTestUtils();
    testLauncher.setJob(job);
    testLauncher.setJobLauncher(jobLauncher);
    testLauncher.setJobRepository(jobRepository);
    return testLauncher;
  }

  protected FileSystemResource actualFileOutput(String spec) throws Exception {
    if (!spec.startsWith("http")) { // Case for CIRCULATION_LOG.
      spec = remoteFilesStorage.objectToPresignedObjectUrl(spec);
    }
    try (InputStream inputStream = new URL(spec).openStream()) {
      final Path actualResult = Files.createTempFile("temp", ".tmp");
      Files.copy(inputStream, actualResult, StandardCopyOption.REPLACE_EXISTING);
      return new FileSystemResource(actualResult);
    }
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }

}
