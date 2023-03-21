package org.folio.dew.batch.authoritycontrol.readers;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.authority.control.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authoritycontrol.DataStatCollectionDTO;
import org.folio.dew.domain.dto.authoritycontrol.DataStatDTO;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

@StepScope
public abstract class AuthorityControlItemReader<T extends DataStatDTO> extends AbstractItemCountingItemStreamItemReader<T> {
  protected final EntitiesLinksStatsClient entitiesLinksStatsClient;
  private final int limit;
  private final OffsetDateTime fromDate;
  private OffsetDateTime toDate;
  private int currentChunkOffset;
  private List<T> currentChunk;

  public AuthorityControlItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                    AuthorityControlExportConfig exportConfig,
                                    AuthorityControlJobProperties jobProperties) {
    this.entitiesLinksStatsClient = entitiesLinksStatsClient;
    this.limit = jobProperties.getEntitiesLinksChunkSize();
    this.fromDate = OffsetDateTime.of(exportConfig.getFromDate(), LocalTime.MIN, ZoneOffset.UTC);
    this.toDate = OffsetDateTime.of(exportConfig.getToDate(), LocalTime.MAX, ZoneOffset.UTC);

    setSaveState(false);
    setCurrentItemCount(0);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  @SuppressWarnings("unchecked")
  protected T doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      if (toDate == null) {
        return null;
      }
      var collection = getCollection(limit);
      currentChunk = (List<T>) collection.getStats();
      toDate = collection.getNext();
      currentChunkOffset = 0;
    }

    if (currentChunk.isEmpty()) {
      return null;
    }

    return currentChunk.get(currentChunkOffset++);
  }

  protected abstract DataStatCollectionDTO getCollection(int limit);

  protected OffsetDateTime fromDate() {
    return fromDate;
  }

  protected OffsetDateTime toDate() {
    return toDate;
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
