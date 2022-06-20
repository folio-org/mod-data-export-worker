package org.folio.dew.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.folio.dew.config.kafka.KafkaService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  private final KafkaService kafkaService;
  private final BulkEditConfigurationService configurationService;

  public FolioTenantService(JdbcTemplate jdbcTemplate,
                            BulkEditConfigurationService configurationService,
                            KafkaService kafkaTopicsInitializer,
                            FolioExecutionContext context,
                            FolioSpringLiquibase folioSpringLiquibase) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.kafkaService = kafkaTopicsInitializer;
    this.configurationService = configurationService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      kafkaService.createKafkaTopics();
      kafkaService.restartEventListeners();
      configurationService.checkBulkEditConfiguration();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
