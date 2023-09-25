package org.folio.dew.batch.acquisitions.edifact.config;

import org.folio.dew.batch.acquisitions.edifact.CompositePOConverter;
import org.folio.dew.batch.acquisitions.edifact.CompositePOLineConverter;
import org.folio.dew.batch.acquisitions.edifact.PurchaseOrdersToEdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({ "org.folio.dew.batch.acquisitions.edifact" })
public class EdifactPurchaseOrderConfig {
  @Bean
  CompositePOLineConverter compositePOLineConverter() {
    return new CompositePOLineConverter();
  }

  @Bean
  CompositePOConverter compositePurchaseOrderConverter(CompositePOLineConverter compositePOLineConverter, ConfigurationService configurationService) {
    return new CompositePOConverter(compositePOLineConverter, configurationService);
  }

  @Bean
  PurchaseOrdersToEdifactMapper mappingOrdersToEdifact(CompositePOConverter compositePOConverter) {
    return new PurchaseOrdersToEdifactMapper(compositePOConverter);
  }
}
