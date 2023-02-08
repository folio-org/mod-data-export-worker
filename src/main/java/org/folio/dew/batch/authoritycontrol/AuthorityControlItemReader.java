package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@StepScope
public class AuthorityControlItemReader extends AbstractItemCountingItemStreamItemReader<AuthorityDataStatDto> {

  private int currentChunkOffset;
  private List<AuthorityDataStatDto> currentChunk;
  private final EntitiesLinksStatsClient entitiesLinksStatsClient;
  private final String fromDate;
  private final String toDate;
  private final int limit;

  protected AuthorityControlItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                       AuthorityControlExportConfig exportConfig,
                                       AuthorityControlJobProperties jobProperties) {
    this.limit = jobProperties.getEntitiesLinksChunkSize();
    this.entitiesLinksStatsClient = entitiesLinksStatsClient;
    this.fromDate = exportConfig.getFromDate().toString();
    this.toDate = exportConfig.getToDate().toString();

    setSaveState(false);
    setCurrentItemCount(0);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected AuthorityDataStatDto doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getItems(limit);
      currentChunkOffset = 0;
    }

    if (currentChunk.isEmpty()) {
      return null;
    }

    currentChunkOffset++;
    return currentChunk.get(currentChunkOffset);
  }

  protected List<AuthorityDataStatDto> getItems(int limit) {
    return entitiesLinksStatsClient
      .getAuthorityStats(limit, fromDate, toDate)
      .getStats();
  }

  @Override
  protected void doOpen() {
    // Nothing to do
  }

  @Override
  protected void doClose() {
    // Nothing to do
  }
}