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

import java.io.Serializable;
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

    private static final Map<String, JobParameter> uniqueDeserializedJobParams = new HashMap<>();

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

        if (identifying){
          return checkForExisted(value, clazz, identifying);
        }

        return new JobParameter<>(value, clazz, identifying);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException("Cannot create Job parameter with the class " + type, e);
      } catch (ParseException e) {
        throw new RuntimeException("Invalid Date format: " + node.asText(),  e);
      }
    }

    /**
     * The spring-batch-core-5.1.1 version brought the changes into job parameters generation approach:
     * previously used spring-batch-core-5.0.3 version builds the following job param set
     * {
     *   "empty": false,
     *   "parameters": {
     *     "jobId": {
     *       "type": "java.lang.String",
     *       "value": "95f8e650-9674-4e64-8252-cd29d4f7d5b5",
     *       "identifying": true
     *     },
     *     "tempOutputFilePath": {
     *       "type": "java.lang.String",
     *       "value": "mod-data-export-worker/bulk_edit/BULK_EDIT_IDENTIFIERS_2024-03-05_14:05:21_95f8e650-9674-4e64-8252-cd29d4f7d5b5",
     *       "identifying": false
     *     }
     *   }
     * }
     * spring-batch-core-5.1.1 version job param set =
     *{
     * 	"empty": false,
     * 	"parameters": {
     * 		"jobId": {
     * 			"type": "java.lang.String",
     * 			"value": "5c6a9246-257d-42c7-a6c2-143f498b5b77",
     * 			"identifying": true
     *                },
     * 		"tempOutputFilePath": {
     * 			"type": "java.lang.String",
     * 			"value": "mod-data-export-worker/bulk_edit/BULK_EDIT_IDENTIFIERS_2024-03-04_19:48:05_5c6a9246-257d-42c7-a6c2-143f498b5b77",
     * 			"identifying": false
     *        }* 	},
     * 	"identifyingParameters": {
     * 		"jobId": {
     * 			"type": "java.lang.String",
     * 			"value": "5c6a9246-257d-42c7-a6c2-143f498b5b77",
     * 			"identifying": true
     * 		}
     *    }
     * }
     * the MapDeserialiser (jackson-databind-2.15.4) uses the JobParameter instances produced by JobParameterDeserializer
     * in the following manner:
     * it checks the equality of instances produced in the middle of internal flow by the "!=" approach
     * and such the verification throws the error for the cases, identifying JobParameters are equal by the content but different objects by the definition.
     * So current approach aimed not to generate brand new identifying JobParameter from identifyingParameters set
     * but use the previously generated one from the parameters.
     * Such the workaround makes able to avoid issues, passing spring-batch-5.1.1 JobParameters generation and jackson-databind-2.15.4 deserialization flows.
     * */
    private JobParameter<?> checkForExisted(Serializable value, Class<Object> clazz, boolean identifying) {
      var existed = uniqueDeserializedJobParams.get(String.valueOf(value));
      if (existed == null){
        var jobParameter = new JobParameter<>(value, clazz, identifying);
        uniqueDeserializedJobParams.put(String.valueOf(value), jobParameter);
        return jobParameter;
      }
      return existed;
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
