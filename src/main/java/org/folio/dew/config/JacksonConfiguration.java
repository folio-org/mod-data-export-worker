package org.folio.dew.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vladmihalcea.hibernate.type.util.ObjectMapperSupplier;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class JacksonConfiguration implements ObjectMapperSupplier {

  private static final ObjectMapper OBJECT_MAPPER;
  private static final ObjectMapper EDI_OBJECT_MAPPER;

  static {
    OBJECT_MAPPER =
        new ObjectMapper()
            .registerModule(
                new SimpleModule()
                    .addDeserializer(ExitStatus.class, new ExitStatusDeserializer())
                    .addDeserializer(JobParameter.class, new JobParameterDeserializer()))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    EDI_OBJECT_MAPPER =
        new ObjectMapper().findAndRegisterModules();
  }

  static class ExitStatusDeserializer extends StdDeserializer<ExitStatus> {

    private static final Map<String, ExitStatus> EXIT_STATUSES = new HashMap<>();

    static {
      EXIT_STATUSES.put("UNKNOWN", ExitStatus.UNKNOWN);
      EXIT_STATUSES.put("EXECUTING", ExitStatus.EXECUTING);
      EXIT_STATUSES.put("COMPLETED", ExitStatus.COMPLETED);
      EXIT_STATUSES.put("NOOP", ExitStatus.NOOP);
      EXIT_STATUSES.put("FAILED", ExitStatus.FAILED);
      EXIT_STATUSES.put("STOPPED", ExitStatus.STOPPED);
    }

    public ExitStatusDeserializer() {
      this(null);
    }

    public ExitStatusDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public ExitStatus deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      return EXIT_STATUSES.get(((JsonNode) jp.getCodec().readTree(jp)).get("exitCode").asText());
    }

  }

  static class JobParameterDeserializer extends StdDeserializer<JobParameter> {

    private static final String VALUE_PARAMETER_PROPERTY = "value";

    public JobParameterDeserializer() {
      this(null);
    }

    public JobParameterDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public JobParameter deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      JsonNode jsonNode = jp.getCodec().readTree(jp);
      var identifying = jsonNode.get("identifying").asBoolean();
      switch (JobParameter.ParameterType.valueOf(jsonNode.get("type").asText())) {
        case STRING:
          return new JobParameter(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText(), identifying);
        case DATE:
          return new JobParameter(
              Date.valueOf(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText()), identifying);
        case LONG:
          return new JobParameter(jsonNode.get(VALUE_PARAMETER_PROPERTY).asLong(), identifying);
        case DOUBLE:
          return new JobParameter(jsonNode.get(VALUE_PARAMETER_PROPERTY).asDouble(), identifying);
      }
      return null;
    }
  }

  @Bean
  public ObjectMapper objectMapper() {
    return OBJECT_MAPPER;
  }

  @Bean
  public ObjectMapper ediObjectMapper() {
    return EDI_OBJECT_MAPPER;
  }

  @Override
  public ObjectMapper get() {
    return OBJECT_MAPPER;
  }

}
