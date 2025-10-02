package org.folio.dew.service;

import java.sql.ResultSet;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.dew.config.kafka.KafkaService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.liquibase.FolioSpringLiquibase;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@Primary
public class FolioTenantService extends TenantService {

  private static final String EXIST_SQL =
    "SELECT EXISTS(SELECT 1 FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?)";

  private final KafkaService kafkaService;
  private final UserTenantsService userTenantsService;

  public FolioTenantService(JdbcTemplate jdbcTemplate,
                            KafkaService kafkaTopicsInitializer,
                            UserTenantsService userTenantsService,
                            FolioExecutionContext context,
                            FolioSpringLiquibase folioSpringLiquibase) {
    super(jdbcTemplate, context, folioSpringLiquibase);
    this.kafkaService = kafkaTopicsInitializer;
    this.userTenantsService = userTenantsService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    try {
      kafkaService.createKafkaTopics();
      kafkaService.restartEventListeners();
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

  /**
   * Check if current context tenant is a part of consortium
   * */
  public boolean isConsortiumTenant() {
    var centralTenant =  userTenantsService.getCentralTenant(context.getTenantId());
    return centralTenant.isPresent() && StringUtils.isNotEmpty(centralTenant.get());
  }
}
