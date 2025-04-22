package org.folio.dew.batch.acquisitions.config;

import org.folio.dew.batch.acquisitions.mapper.CsvMapper;
import org.folio.dew.batch.acquisitions.mapper.ExportResourceMapper;
import org.folio.dew.batch.acquisitions.mapper.converter.CompOrderEdiConverter;
import org.folio.dew.batch.acquisitions.mapper.converter.PoLineEdiConverter;
import org.folio.dew.batch.acquisitions.mapper.EdifactMapper;
import org.folio.dew.batch.acquisitions.services.ConfigurationService;
import org.folio.dew.batch.acquisitions.services.ExpenseClassService;
import org.folio.dew.batch.acquisitions.services.HoldingService;
import org.folio.dew.batch.acquisitions.services.IdentifierTypeService;
import org.folio.dew.batch.acquisitions.services.LocationService;
import org.folio.dew.batch.acquisitions.services.MaterialTypeService;
import org.folio.dew.batch.acquisitions.services.OrdersService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({ "org.folio.dew.batch.acquisitions.edifact" })
public class AcquisitionExportConfig {

  @Bean
  PoLineEdiConverter poLineConverter(IdentifierTypeService identifierTypeService, MaterialTypeService materialTypeService,
                                     ExpenseClassService expenseClassService, LocationService locationService, HoldingService holdingService) {
    return new PoLineEdiConverter(identifierTypeService, materialTypeService, expenseClassService, locationService, holdingService);
  }

  @Bean
  CompOrderEdiConverter compositePurchaseOrderConverter(PoLineEdiConverter poLineEdiConverter, ConfigurationService configurationService) {
    return new CompOrderEdiConverter(poLineEdiConverter, configurationService);
  }

  @Bean
  ExportResourceMapper edifactMapper(CompOrderEdiConverter compOrderEdiConverter) {
    return new EdifactMapper(compOrderEdiConverter);
  }

  @Bean
  ExportResourceMapper csvMapper(OrdersService ordersService) {
    return new CsvMapper(ordersService);
  }

}
