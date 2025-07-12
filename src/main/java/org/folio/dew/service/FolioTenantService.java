package org.folio.dew.service;

import java.sql.ResultSet;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
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

  private static final String EXIST_SQL =
    "SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?)";

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
      configurationService.updateBulkEditConfiguration();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Implemented by HSQLDB way
   * Check if the tenant exists (by way of its database schema)
   * @return if the tenant's database schema exists
   */
  @Override
  protected boolean tenantExists() {
    return BooleanUtils.isTrue(
      jdbcTemplate.query(EXIST_SQL,
        (ResultSet resultSet) -> resultSet.next() && resultSet.getBoolean(1),
        getSchemaName()
      )
    );
  }
}
