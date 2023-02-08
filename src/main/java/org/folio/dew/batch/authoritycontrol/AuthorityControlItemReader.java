package org.folio.dew.batch.authoritycontrol;

import org.folio.dew.client.EntitiesLinksStatsClient;
import org.folio.dew.config.properties.AuthorityControlJobProperties;
import org.folio.dew.domain.dto.AuthorityControlExportConfig;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDto;
import org.folio.dew.domain.dto.authority.control.AuthorityDataStatDtoCollection;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@StepScope
public class AuthorityControlItemReader extends AbstractItemCountingItemStreamItemReader<AuthorityDataStatDto> {

  private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
  private final EntitiesLinksStatsClient entitiesLinksStatsClient;
  private final int limit;
  private final Date toDate;
  private Date fromDate;
  private int currentChunkOffset;
  private List<AuthorityDataStatDto> currentChunk;

  protected AuthorityControlItemReader(EntitiesLinksStatsClient entitiesLinksStatsClient,
                                       AuthorityControlExportConfig exportConfig,
                                       AuthorityControlJobProperties jobProperties) {
    this.limit = jobProperties.getEntitiesLinksChunkSize();
    this.entitiesLinksStatsClient = entitiesLinksStatsClient;
    this.fromDate = exportConfig.getFromDate();
    this.toDate = exportConfig.getToDate();

    setSaveState(false);
    setCurrentItemCount(0);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected AuthorityDataStatDto doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      var collection = getItems(limit);
      currentChunk = collection.getStats();
      fromDate = collection.getNext();
      currentChunkOffset = 0;
    }

    if (currentChunk.isEmpty()) {
      return null;
    }

    return currentChunk.get(currentChunkOffset++);
  }

  protected AuthorityDataStatDtoCollection getItems(int limit) {
    return entitiesLinksStatsClient
      .getAuthorityStats(limit, DATE_FORMAT.format(fromDate), DATE_FORMAT.format(toDate));
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
