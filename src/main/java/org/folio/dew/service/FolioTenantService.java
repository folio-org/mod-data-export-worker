package org.folio.dew.service;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import org.folio.dew.config.kafka.KafkaService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;

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
    kafkaService.createKafkaTopics();
    kafkaService.restartEventListeners();
    configurationService.checkBulkEditConfiguration();
  }
}
