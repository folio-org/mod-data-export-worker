package org.folio.dew;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@SpringBootApplication
@EnableFeignClients(basePackages = "org.folio.dew.client")
@EnableBatchProcessing(isolationLevelForCreate = "ISOLATION_READ_COMMITTED")
@EntityScan("org.folio.de.entity")
public class ModDataExportWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModDataExportWorkerApplication.class, args);
  }

  @Bean
  @Primary
  public ObjectMapper primaryObjectMapper() {
    return JsonMapper.builder()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .addModule(new JavaTimeModule()).build();
  }
}
