package org.folio.dew.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.batch.core.JobParameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.sql.Date;

@Configuration
public class JacksonConfiguration {

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
      boolean identifying = jsonNode.get("identifying").asBoolean();
      switch (JobParameter.ParameterType.valueOf(jsonNode.get("type").asText())) {
      case STRING:
        return new JobParameter(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText(), identifying);
      case DATE:
        return new JobParameter(Date.valueOf(jsonNode.get(VALUE_PARAMETER_PROPERTY).asText()), identifying);
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
    return new ObjectMapper().registerModule(new SimpleModule().addDeserializer(JobParameter.class, new JobParameterDeserializer()))
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

}
