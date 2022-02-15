package org.folio.dew;

import org.folio.dew.batch.acquisitions.edifact.client.ExpenseClassClient;
import org.folio.dew.batch.acquisitions.edifact.client.HoldingClient;
import org.folio.dew.batch.acquisitions.edifact.client.IdentifierTypeClient;
import org.folio.dew.batch.acquisitions.edifact.client.LocationClient;
import org.folio.dew.batch.acquisitions.edifact.client.MaterialTypeClient;
import org.folio.dew.client.AccountBulkClient;
import org.folio.dew.client.AccountClient;
import org.folio.dew.client.AddressTypeClient;
import org.folio.dew.client.AuditClient;
import org.folio.dew.client.ConfigurationClient;
import org.folio.dew.client.DataExportSpringClient;
import org.folio.dew.client.DepartmentClient;
import org.folio.dew.client.FeefineactionsClient;
import org.folio.dew.client.GroupClient;
import org.folio.dew.client.MaterialTypeClient;
import org.folio.dew.client.OrdersClient;
import org.folio.dew.client.OrdersStorageClient;
import org.folio.dew.client.ProxiesForClient;
import org.folio.dew.client.PurchaseOrderLineClient;
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
  AddressTypeClient.class,
  AuditClient.class,
  DepartmentClient.class,
  FeefineactionsClient.class,
  GroupClient.class,
  ProxiesForClient.class,
  ServicePointClient.class,
  TransferClient.class,
  UserClient.class,
  DataExportSpringClient.class,
  MaterialTypeClient.class,
  OrdersClient.class,
  OrdersStorageClient.class,
  PurchaseOrderLineClient.class,
  IdentifierTypeClient.class,
  LocationClient.class,
  HoldingClient.class,
  ExpenseClassClient.class
})
@EnableBatchProcessing
public class ModDataExportWorkerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ModDataExportWorkerApplication.class, args);
  }

}
