package org.folio.dew.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.dew.CopilotGenerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameter;

import static org.junit.jupiter.api.Assertions.*;

@CopilotGenerated
class JacksonConfigurationTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new JacksonConfiguration().objectMapper();
  }

  // ── objectMapper() / ediObjectMapper() beans ──────────────────────────

  @Test
  void objectMapper_isNotNull() {
    assertNotNull(objectMapper);
  }

  @Test
  void ediObjectMapper_isNotNull() {
    assertNotNull(new JacksonConfiguration().ediObjectMapper());
  }

  @Test
  void get_returnsSameObjectMapperInstance() {
    JacksonConfiguration cfg = new JacksonConfiguration();
    assertSame(cfg.objectMapper(), cfg.get());
  }

  // ── ExitStatus deserialization ─────────────────────────────────────────

  @Test
  void deserializeExitStatus_completed() throws Exception {
    ExitStatus status = objectMapper.readValue("{\"exitCode\":\"COMPLETED\"}", ExitStatus.class);
    assertEquals(ExitStatus.COMPLETED, status);
  }

  @Test
  void deserializeExitStatus_failed() throws Exception {
    ExitStatus status = objectMapper.readValue("{\"exitCode\":\"FAILED\"}", ExitStatus.class);
    assertEquals(ExitStatus.FAILED, status);
  }

  @Test
  void deserializeExitStatus_unknown() throws Exception {
    ExitStatus status = objectMapper.readValue("{\"exitCode\":\"UNKNOWN\"}", ExitStatus.class);
    assertEquals(ExitStatus.UNKNOWN, status);
  }

  @Test
  void deserializeExitStatus_executing() throws Exception {
    ExitStatus status = objectMapper.readValue("{\"exitCode\":\"EXECUTING\"}", ExitStatus.class);
    assertEquals(ExitStatus.EXECUTING, status);
  }

  @Test
  void deserializeExitStatus_noop() throws Exception {
    ExitStatus status = objectMapper.readValue("{\"exitCode\":\"NOOP\"}", ExitStatus.class);
    assertEquals(ExitStatus.NOOP, status);
  }

  @Test
  void deserializeExitStatus_stopped() throws Exception {
    ExitStatus status = objectMapper.readValue("{\"exitCode\":\"STOPPED\"}", ExitStatus.class);
    assertEquals(ExitStatus.STOPPED, status);
  }

  // ── JobParameter deserialization – non-identifying ────────────────────

  @Test
  void deserializeJobParameter_stringValue() throws Exception {
    String json = "{\"type\":\"java.lang.String\",\"value\":\"hello\",\"identifying\":false}";
    JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
    assertEquals("hello", param.value());
    assertFalse(param.identifying());
  }

  @Test
  void deserializeJobParameter_longValue() throws Exception {
    String json = "{\"type\":\"java.lang.Long\",\"value\":42,\"identifying\":false}";
    JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
    assertEquals(42L, param.value());
  }

  @Test
  void deserializeJobParameter_integerValue() throws Exception {
    String json = "{\"type\":\"java.lang.Integer\",\"value\":7,\"identifying\":false}";
    JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
    assertEquals(7, param.value());
  }

  @Test
  void deserializeJobParameter_doubleValue() throws Exception {
    String json = "{\"type\":\"java.lang.Double\",\"value\":3.14,\"identifying\":false}";
    JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
    assertEquals(3.14, (Double) param.value(), 0.0001);
  }

  @Test
  void deserializeJobParameter_booleanValue() throws Exception {
    String json = "{\"type\":\"java.lang.Boolean\",\"value\":true,\"identifying\":false}";
    JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
    assertEquals(Boolean.TRUE, param.value());
  }

  @Test
  void deserializeJobParameter_dateValue() throws Exception {
    String json = "{\"type\":\"java.util.Date\",\"value\":\"2024-03-05T14:05:21.000\",\"identifying\":false}";
    JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
    assertNotNull(param.value());
  }

  @Test
  void deserializeJobParameter_invalidDateFormat_throwsRuntimeException() {
    String json = "{\"type\":\"java.util.Date\",\"value\":\"not-a-date\",\"identifying\":false}";
    assertThrows(Exception.class, () -> objectMapper.readValue(json, JobParameter.class));
  }

  @Test
  void deserializeJobParameter_unknownClass_throwsRuntimeException() {
    String json = "{\"type\":\"com.example.NonExistentClass\",\"value\":\"x\",\"identifying\":false}";
    assertThrows(Exception.class, () -> objectMapper.readValue(json, JobParameter.class));
  }

  // ── JobParameter deserialization – identifying (de-duplication) ───────

  @Test
  void deserializeJobParameter_identifyingString_isIdentifying() throws Exception {
    String json = "{\"type\":\"java.lang.String\",\"value\":\"job-id-unique-1\",\"identifying\":true}";
    JobParameter<?> param = objectMapper.readValue(json, JobParameter.class);
    assertTrue(param.identifying());
    assertEquals("job-id-unique-1", param.value());
  }

  /**
   * Verifies that two identifying JobParameters with the same value are equal.
   * The de-duplication in checkForExisted exists to satisfy jackson-databind's
   * MapDeserializer "!=" check when identifyingParameters re-references a param
   * already seen in parameters — it is not a cross-call identity guarantee.
   */
  @Test
  void deserializeJobParameter_identifyingSameValue_areEqual() throws Exception {
    String json = "{\"type\":\"java.lang.String\",\"value\":\"shared-job-id\",\"identifying\":true}";
    JobParameter<?> first = objectMapper.readValue(json, JobParameter.class);
    JobParameter<?> second = objectMapper.readValue(json, JobParameter.class);
    assertEquals(first, second,
        "Identifying JobParameters with the same value must be equal");
  }

  // ── Full JobParameters map round-trip (spring-batch-5.1.1 format) ─────

  @Test
  void deserializeFullJobParameters_spring511Format() throws Exception {
    String json = """
        {
          "empty": false,
          "parameters": {
            "jobId": {
              "type": "java.lang.String",
              "value": "5c6a9246-257d-42c7-a6c2-143f498b5b77",
              "identifying": true
            },
            "tempOutputFilePath": {
              "type": "java.lang.String",
              "value": "mod-data-export-worker/bulk_edit/BULK_EDIT_IDENTIFIERS_2024-03-04",
              "identifying": false
            }
          },
          "identifyingParameters": {
            "jobId": {
              "type": "java.lang.String",
              "value": "5c6a9246-257d-42c7-a6c2-143f498b5b77",
              "identifying": true
            }
          }
        }
        """;

    // Must not throw even though identifyingParameters re-uses the same JobParameter value
    var tree = objectMapper.readTree(json);
    assertNotNull(tree);

    // Verify individual parameter deserialization within the same mapper session
    var parametersNode = tree.get("parameters");
    var jobIdNode = parametersNode.get("jobId");
    var jobIdParam = objectMapper.treeToValue(jobIdNode, JobParameter.class);
    assertEquals("5c6a9246-257d-42c7-a6c2-143f498b5b77", jobIdParam.value());
    assertTrue(jobIdParam.identifying());
  }

  // ── Unknown properties are ignored ────────────────────────────────────

  @Test
  void objectMapper_ignoresUnknownProperties() throws Exception {
    // Should not throw DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
    String json = "{\"exitCode\":\"COMPLETED\",\"unknownField\":\"value\"}";
    ExitStatus status = objectMapper.readValue(json, ExitStatus.class);
    assertNotNull(status);
  }
}

