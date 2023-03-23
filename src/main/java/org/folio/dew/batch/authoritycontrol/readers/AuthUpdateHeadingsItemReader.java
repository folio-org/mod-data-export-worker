package org.folio.dew.batch.authoritycontrol.readers;

import static org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto.ActionEnum.UPDATE_HEADING;

import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDtoCollection;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class AuthUpdateHeadingsItemReader extends AuthorityControlItemReader<AuthorityDataStatDto> {

  public AuthUpdateHeadingsItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                      AuthorityControlExportConfig exportConfig,
                                      AuthorityControlJobProperties jobProperties) {
    super(entitiesLinksStatsClient, exportConfig, jobProperties);
  }

  @Override
  protected AuthorityDataStatDtoCollection getCollection(int limit) {
    return entitiesLinksStatsClient.getAuthorityStats(limit, UPDATE_HEADING, fromDate(), toDate());
  }
}
