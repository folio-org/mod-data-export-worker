package org.folio.dew.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
@Log4j2
public class JacksonConfiguration {

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
//                    .addDeserializer(JobParameters.class, new JobParametersDeserializer()))
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

//  static class JobParametersDeserializer extends StdDeserializer<JobParameters> {
//
//    public JobParametersDeserializer() {
//      this(null);
//    }
//
//    public JobParametersDeserializer(Class<?> vc) {
//      super(vc);
//    }
//
//    @Override
//    public JobParameters deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
//      JsonNode node = jp.getCodec().readTree(jp);
//      Set<JobParameter<?>> paramSet = new HashSet<>();
//
//      JsonNode parametersNode = node.get("parameters");
//      log.info("node: {}, parametersNode: {}", node, parametersNode);
//      if (parametersNode != null && parametersNode.isObject()) {
//        parametersNode.fields().forEachRemaining(entry -> {
//          JsonNode paramNode = entry.getValue();
//          String type = paramNode.has("type") ? paramNode.get("type").asText() : "java.lang.String";
//          boolean identifying = !paramNode.has("identifying") || paramNode.get("identifying").asBoolean(true);
//          var valueNode = paramNode.get("value");
//          try {
//            Class<Object> clazz = (Class<Object>) Class.forName(type);
//            Object value = switch (clazz.getSimpleName()) {
//              case "Double" -> valueNode.asDouble();
//              case "Long" -> valueNode.asLong();
//              case "Integer" -> valueNode.asInt();
//              case "Boolean" -> valueNode.asBoolean();
//              case "Date" -> new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(valueNode.asText());
//              default -> valueNode.asText();
//            };
//            paramSet.add(new JobParameter<>(entry.getKey(), value, clazz, identifying));
//          } catch (ClassNotFoundException | ParseException e) {
//            throw new RuntimeException("Failed to deserialize JobParameter: " + entry.getKey(), e);
//          }
//        });
//      }
//
//      return new JobParameters(paramSet);
//    }
//
//  }

  static class JobParameterDeserializer extends StdDeserializer<JobParameter<?>> {

    private static final String VALUE_PARAMETER_PROPERTY = "value";

    private static final Set<JobParameter<?>> uniqueDeserializedJobParams = new HashSet<>();

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

        return new JobParameter<>(clazzSimpleName, value, clazz, identifying);
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
      var existed = uniqueDeserializedJobParams.stream().filter(jp -> jp.value().equals(String.valueOf(value))).findFirst();
      if (existed.isEmpty()){
        var jobParameter = new JobParameter<>(clazz.getSimpleName(), value, clazz, identifying);
        uniqueDeserializedJobParams.add(jobParameter);
        return jobParameter;
      }
      return existed.get();
    }
  }

  @Primary
  @Bean
  public ObjectMapper objectMapper() {
    return OBJECT_MAPPER;
  }

  @Bean
  public ObjectMapper ediObjectMapper() {
    return EDI_OBJECT_MAPPER;
  }

  public ObjectMapper get() {
    return OBJECT_MAPPER;
  }

}
