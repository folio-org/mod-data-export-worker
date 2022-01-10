package org.folio.dew.batch.acquisitions.edifact.config;

import org.folio.dew.batch.acquisitions.edifact.CompositePOLineConverter;
import org.folio.dew.batch.acquisitions.edifact.CompositePOConverter;
import org.folio.dew.batch.acquisitions.edifact.MappingOrdersToEdifact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({ "org.folio" })
public class EdifactPurchaseOrderConfig {
  @Bean
  CompositePOLineConverter compositePOLineConverter() {
    return new CompositePOLineConverter();
  }

  @Bean CompositePOConverter compositePurchaseOrderConverter(CompositePOLineConverter compositePOLineConverter) {
    return new CompositePOConverter(compositePOLineConverter);
  }

  @Bean
  MappingOrdersToEdifact mappingOrdersToEdifact(CompositePOConverter compositePOConverter) {
    return new MappingOrdersToEdifact(compositePOConverter);
  }
}
