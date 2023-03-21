package org.folio.dew.batch.authoritycontrol.readers;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.client.EntitiesLinksStatsClient.LinkStatus;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDto;
import org.folio.dew.domain.dto.authority.control.InstanceDataStatDtoCollection;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class LinkedBibUpdateItemReader extends AbstractItemCountingItemStreamItemReader<InstanceDataStatDto> {

  private final EntitiesLinksStatsClient entitiesLinksStatsClient;
  private final int limit;
  private final OffsetDateTime fromDate;
  private OffsetDateTime toDate;
  private int currentChunkOffset;
  private List<InstanceDataStatDto> currentChunk;

  protected LinkedBibUpdateItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                      AuthorityControlExportConfig exportConfig,
                                      AuthorityControlJobProperties jobProperties) {
    this.limit = jobProperties.getEntitiesLinksChunkSize();
    this.entitiesLinksStatsClient = entitiesLinksStatsClient;
    this.fromDate = OffsetDateTime.of(exportConfig.getFromDate(), LocalTime.MIN, ZoneOffset.UTC);
    this.toDate = OffsetDateTime.of(exportConfig.getToDate(), LocalTime.MAX, ZoneOffset.UTC);

    setSaveState(false);
    setCurrentItemCount(0);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected InstanceDataStatDto doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      if (toDate == null) {
        return null;
      }
      var collection = getItems(limit);
      currentChunk = collection.getStats();
      toDate = collection.getNext();
      currentChunkOffset = 0;
    }

    if (currentChunk.isEmpty()) {
      return null;
    }

    return currentChunk.get(currentChunkOffset++);
  }

  protected InstanceDataStatDtoCollection getItems(int limit) {
    return entitiesLinksStatsClient.getInstanceStats(limit, LinkStatus.ERROR, fromDate.toString(), toDate.toString());
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
