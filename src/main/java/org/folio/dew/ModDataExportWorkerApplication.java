package org.folio.dew;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ModDataExportWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModDataExportWorkerApplication.class, args);
  }

}
