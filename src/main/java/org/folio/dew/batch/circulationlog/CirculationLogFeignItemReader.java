package org.folio.dew.batch.circulationlog;

import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.LogRecord;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

import java.util.List;
import java.util.UUID;

public class CirculationLogFeignItemReader extends AbstractItemCountingItemStreamItemReader<LogRecord> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 100;

  private final AuditClient auditClient;
  private final String query;
  private int currentOffset;

  private List<LogRecord> currentChunk;
  private int currentChunkOffset;

  public CirculationLogFeignItemReader(AuditClient auditClient, String query, Long offset, Long limit) {
    this.auditClient = auditClient;
    this.query = query;
    currentOffset = offset.intValue();

    setCurrentItemCount(0);
    setMaxItemCount(limit.intValue());
    setSaveState(false);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected LogRecord doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getLogRecords();
      currentOffset += QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST;
      currentChunkOffset = 0;
    }

    if (currentChunk.isEmpty()) {
      return null;
    }

    LogRecord logRecord = currentChunk.get(currentChunkOffset);
    currentChunkOffset++;

    return logRecord;
  }

  @Override
  protected void doOpen() {
    // Nothing to do
  }

  @Override
  protected void doClose() {
    // Nothing to do
  }

  private List<LogRecord> getLogRecords() {
    return auditClient.getCirculationAuditLogs(query, currentOffset, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST, null).getLogRecords();
  }

}
