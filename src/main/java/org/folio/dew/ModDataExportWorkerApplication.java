package org.folio.dew;

import org.folio.des.client.ConfigurationClient;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.AuditClient;
import org.folio.dew.client.FeefineactionsClient;
import org.folio.dew.client.ServicePointClient;
import org.folio.dew.client.TransferClient;
import org.folio.dew.client.UserClient;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {
  ConfigurationClient.class,
  AccountBulkClient.class,
  AccountClient.class,
  AuditClient.class,
  FeefineactionsClient.class,
  ServicePointClient.class,
  TransferClient.class,
  UserClient.class
})
@EnableBatchProcessing
public class ModDataExportWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModDataExportWorkerApplication.class, args);
  }

}
