package org.folio.dew;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.transaction.annotation.Isolation;

@SpringBootApplication(scanBasePackages = {
  "org.folio.dew",
  "org.folio.spring.scope"
})
@EnableBatchProcessing
@EnableJdbcJobRepository(isolationLevelForCreate = Isolation.READ_COMMITTED)
@EntityScan("org.folio.de.entity")
public class ModDataExportWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModDataExportWorkerApplication.class, args);
  }

}
