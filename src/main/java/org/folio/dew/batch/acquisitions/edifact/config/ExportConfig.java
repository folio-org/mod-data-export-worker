package org.folio.dew.batch.acquisitions.edifact.config;

import org.folio.dew.batch.acquisitions.edifact.mapper.CsvMapper;
import org.folio.dew.batch.acquisitions.edifact.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.CompOrderEdiConverter;
import org.folio.dew.batch.acquisitions.edifact.mapper.converter.CompPoLineEdiConverter;
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
public class ExportConfig {

  @Bean
  CompPoLineEdiConverter compositePOLineConverter(IdentifierTypeService identifierTypeService, MaterialTypeService materialTypeService,
                                                  ExpenseClassService expenseClassService, LocationService locationService, HoldingService holdingService) {
    return new CompPoLineEdiConverter(identifierTypeService, materialTypeService, expenseClassService, locationService, holdingService);
  }

  @Bean
  CompOrderEdiConverter compositePurchaseOrderConverter(CompPoLineEdiConverter compPoLineEdiConverter, ConfigurationService configurationService) {
    return new CompOrderEdiConverter(compPoLineEdiConverter, configurationService);
  }

  @Bean
  ExportResourceMapper edifactMapper(CompOrderEdiConverter compOrderEdiConverter) {
    return new EdifactMapper(compOrderEdiConverter);
  }

  @Bean
  ExportResourceMapper csvMapper() {
    return new CsvMapper();
  }

}
