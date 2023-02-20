package org.folio.dew.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.hypersistence.utils.hibernate.type.util.ObjectMapperSupplier;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobParameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class JacksonConfiguration implements ObjectMapperSupplier {

  private static final ObjectMapper OBJECT_MAPPER;
  private static final ObjectMapper EDI_OBJECT_MAPPER;

  static {
    OBJECT_MAPPER =
        new ObjectMapper()
            .registerModule(new JavaTimeModule())
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

  static class JobParameterDeserializer extends StdDeserializer<JobParameter<?>> {

    private static final String VALUE_PARAMETER_PROPERTY = "value";

    public JobParameterDeserializer() {
      this(null);
    }

    public JobParameterDeserializer(Class<?> vc) {
      super(vc);
    }

    @Override
    public JobParameter<?> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      JsonNode jsonNode = jp.getCodec().readTree(jp);
      var identifying = jsonNode.get("identifying").asBoolean();
      var type = jsonNode.get("type").asText();
      var node = jsonNode.get(VALUE_PARAMETER_PROPERTY);

      try {
        Class<Object> clazz = (Class<Object>) Class.forName(type);
        var clazzSimpleName = clazz.getSimpleName();
        var value = switch (clazzSimpleName) {
          case "Double" -> node.asDouble();
          case "Long" -> node.asLong();
          case "Integer" -> node.asInt();
          case "Boolean" -> node.asBoolean();
          // We still do not have Date parameters for jobs, so this could be changed
          // See org.folio.des.builder.job.JobCommandBuilder implementations in mod-data-export-spring
          case "Date" -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(node.asText());
          default -> node.asText();
        };

        return new JobParameter<>(value, clazz, identifying);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Cannot create Job parameter with the class " + type, e);
      } catch (ParseException e) {
        throw new RuntimeException("Invalid Date format: " + node.asText(),  e);
      }
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
