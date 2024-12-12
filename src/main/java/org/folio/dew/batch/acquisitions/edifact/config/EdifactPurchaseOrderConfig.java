package org.folio.dew.batch.acquisitions.edifact.config;

import org.folio.dew.batch.acquisitions.edifact.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.CompositePOConverter;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.CompositePOLineConverter;
import org.folio.dew.batch.acquisitions.edifact.mapper.EdifactMapper;
import org.folio.dew.batch.acquisitions.edifact.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.edifact.services.ExpenseClassService;
import org.folio.dew.batch.acquisitions.edifact.services.HoldingService;
import org.folio.dew.batch.acquisitions.edifact.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.edifact.services.LocationService;
import org.folio.dew.batch.acquisitions.edifact.services.MaterialTypeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({ "org.folio.dew.batch.acquisitions.edifact" })
public class EdifactPurchaseOrderConfig {
  @Bean
  CompositePOLineConverter compositePOLineConverter(IdentifierTypeService identifierTypeService, MaterialTypeService materialTypeService,
                                                    ExpenseClassService expenseClassService, LocationService locationService, HoldingService holdingService) {
    return new CompositePOLineConverter(identifierTypeService, materialTypeService, expenseClassService, locationService, holdingService);
  }

  @Bean
  CompositePOConverter compositePurchaseOrderConverter(CompositePOLineConverter compositePOLineConverter, ConfigurationService configurationService) {
    return new CompositePOConverter(compositePOLineConverter, configurationService);
  }

  @Bean
  ExportResourceMapper mappingOrdersToEdifact(CompositePOConverter compositePOConverter) {
    return new EdifactMapper(compositePOConverter);
  }
}
