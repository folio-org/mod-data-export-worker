package org.folio.dew;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.folio.spring.integration.XOkapiHeaders.URL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.folio.dew.client.UserTenantsClient;
import org.folio.dew.config.HttpClientConfiguration;
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
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.liquibase.enabled=true"})
@ContextConfiguration(initializers = BaseBatchTest.DockerPostgreDataSourceInitializer.class)
@Testcontainers
@AutoConfigureMockMvc
@ImportAutoConfiguration(HttpClientConfiguration.class)
@EmbeddedKafka(topics = { "diku.data-export.job.command" })
@EnableBatchProcessing
public abstract class BaseBatchTest {
  protected static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkaWt1X2FkbWluIiwidXNlcl9pZCI6IjFkM2I1OGNiLTA3YjUtNWZjZC04YTJhLTNjZTA2YTBlYjkwZiIsImlhdCI6MTYxNjQyMDM5MywidGVuYW50IjoiZGlrdSJ9.2nvEYQBbJP1PewEgxixBWLHSX_eELiBEBpjufWiJZRs";
  protected static final String NON_CONSORTIUM_TENANT = "diku";
  protected static final String CONSORTIUM_TENANT = "consortium";
  protected static final String CONSORTIUM_MEMBER_TENANT = "college";
  public static final int WIRE_MOCK_PORT = TestSocketUtils.findAvailableTcpPort();

  private static String tenant = NON_CONSORTIUM_TENANT;
  public static WireMockServer wireMockServer;
  public static PostgreSQLContainer<?> postgreDBContainer = new PostgreSQLContainer<>("postgres:16");

  protected static MockMvc mockMvc;
  @Autowired
  private FolioModuleMetadata folioModuleMetadata;
  @Autowired
  protected JobOperator jobOperator;
  @Autowired
  protected JobRepository jobRepository;
  @Autowired
  protected ObjectMapper objectMapper;
  @Autowired
  protected RemoteFilesStorage remoteFilesStorage;
  @MockitoSpyBean
  protected JobCommandsReceiverService jobCommandsReceiverService;
  @MockitoBean
  @Qualifier("exportJobManager")
  protected ExportJobManager exportJobManager;
  @MockitoBean
  @Qualifier("exportJobManagerSync")
  protected ExportJobManagerSync exportJobManagerSync;
  @Value("${spring.application.name}")
  protected String springApplicationName;

  public static final LocalStackContainer localstack = new LocalStackContainer(DockerImageName.parse("localstack/localstack:4.10.0"))
      .withServices(S3)
      .withEnv("EAGER_SERVICE_LOADING", "1");

  static {
    postgreDBContainer.start();
    localstack.start();
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

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry dynamicPropertyRegistry) {
    String endpoint = localstack.getEndpointOverride(S3).toString();
    String region   = localstack.getRegion();
    String access   = localstack.getAccessKey();
    String secret   = localstack.getSecretKey();

    dynamicPropertyRegistry.add("application.minio-local.endpoint", () -> endpoint);
    dynamicPropertyRegistry.add("application.minio-local.region",   () -> region);
    dynamicPropertyRegistry.add("application.minio-local.accessKey",() -> access);
    dynamicPropertyRegistry.add("application.minio-local.secretKey",() -> secret);
    dynamicPropertyRegistry.add("application.minio-remote.subPath",() -> "local");

    dynamicPropertyRegistry.add("application.minio-remote.endpoint", () -> endpoint);
    dynamicPropertyRegistry.add("application.minio-remote.region",   () -> region);
    dynamicPropertyRegistry.add("application.minio-remote.accessKey",() -> access);
    dynamicPropertyRegistry.add("application.minio-remote.secretKey",() -> secret);
    dynamicPropertyRegistry.add("application.minio-remote.subPath",() -> "remote");
  }

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();
    BaseBatchTest.mockMvc = mockMvc;
  }

  @SneakyThrows
  protected static void setUpTenant(String tenant) {
    BaseBatchTest.tenant = tenant;
    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().moduleTo("mod-data-export-worker")))
        .headers(defaultHeaders())
        .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
      .registerModule(new JavaTimeModule());

  @SneakyThrows
  protected static void setUpConsortiumTenant(String centralTenantId,
                                              List<String> memberTenantIds,
                                              String currentTenant) {
    BaseBatchTest.tenant = currentTenant;
    setUpNewTenant(centralTenantId);
    setUpConsortiumMemberTenants(centralTenantId, memberTenantIds);
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(tenant));
    httpHeaders.add(XOkapiHeaders.URL, wireMockServer.baseUrl());
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());

    return httpHeaders;
  }

  @BeforeEach
  protected void setUp() {
    okapiHeaders = new LinkedHashMap<>();
    okapiHeaders.put(XOkapiHeaders.TENANT, tenant);
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
  }

  @AfterEach
  void eachTearDown() {
    folioExecutionContextSetter.close();
  }

  protected JobOperatorTestUtils createTestLauncher(Job job) {
    JobOperatorTestUtils testLauncher = new JobOperatorTestUtils(jobOperator, jobRepository);
    testLauncher.setJob(job);
    return testLauncher;
  }

  protected FileSystemResource actualFileOutput(String spec) throws Exception {
    if (!spec.startsWith("http")) { // Case for CIRCULATION_LOG.
      spec = remoteFilesStorage.objectToPresignedObjectUrl(spec);
    }
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(spec))
          .GET()
          .build();
      try (var inputStream = client.send(request, HttpResponse.BodyHandlers.ofInputStream()).body()) {
        final Path actualResult = Files.createTempFile("temp", ".tmp");
        Files.copy(inputStream, actualResult, StandardCopyOption.REPLACE_EXISTING);
        return new FileSystemResource(actualResult);
      }
    }
  }

  @AfterAll
  static void tearDown() {
    wireMockServer.stop();
  }

  @SneakyThrows
  private static void setUpConsortiumMemberTenants(String centralTenantId,
                                                   List<String> memberTenantIds) {
    memberTenantIds.forEach(BaseBatchTest::setUpNewTenant);
    var consortiumId = UUID.randomUUID().toString();
    var userTenants = new UserTenantsClient.UserTenants(
      List.of(new UserTenantsClient.UserTenant(centralTenantId, consortiumId)));
    mockGet("/user-tenants", OBJECT_MAPPER.writeValueAsString(userTenants), SC_OK, wireMockServer);
    var consortiumTenantList = memberTenantIds.stream()
      .map(s -> new ConsortiumTenant(s, false))
      .collect(Collectors.toList());
    consortiumTenantList.add(new ConsortiumTenant(centralTenantId, true));
    var consortiumTenants = new ConsortiumTenants(consortiumTenantList);
    mockGet("/consortia/" + consortiumId + "/tenants", OBJECT_MAPPER.writeValueAsString(consortiumTenants), SC_OK,
      wireMockServer);
  }

  @SneakyThrows
  private static void setUpNewTenant(String tenant) {
    var httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(TENANT, tenant);
    httpHeaders.add(XOkapiHeaders.USER_ID, UUID.randomUUID().toString());
    httpHeaders.add(URL, wireMockServer.baseUrl());

    mockMvc.perform(post("/_/tenant").content(asJsonString(new TenantAttributes().moduleTo("mod-data-export-worker")))
      .headers(httpHeaders)
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
    mockGet("/user-tenants", "{\"userTenants\": [],\"totalRecords\": 0}", SC_OK, wireMockServer);
  }

  private static void mockGet(String url, String body, int status, WireMockServer mockServer) {
    mockServer.stubFor(WireMock.get(urlPathEqualTo(url))
      .willReturn(aResponse().withBody(body)
        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .withStatus(status)));
  }

  record ConsortiumTenant(String id, boolean isCentral) {
  }

  record ConsortiumTenants(List<ConsortiumTenant> tenants) {
  }
}
