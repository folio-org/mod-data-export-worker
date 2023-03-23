package org.folio.dew.batch.authoritycontrol.readers;

import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.client.EntitiesLinksStatsClient.LinkStatus;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDto;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDtoCollection;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

@StepScope
@Component
public class LinkedBibUpdateItemReader extends AuthorityControlItemReader<InstanceDataStatDto> {

  public LinkedBibUpdateItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                   AuthorityControlExportConfig exportConfig,
                                   AuthorityControlJobProperties jobProperties) {
    super(entitiesLinksStatsClient, exportConfig, jobProperties);
  }

  @Override
  protected InstanceDataStatDtoCollection getCollection(int limit) {
    return entitiesLinksStatsClient.getInstanceStats(limit, LinkStatus.ERROR, fromDate(), toDate());
  }
}
