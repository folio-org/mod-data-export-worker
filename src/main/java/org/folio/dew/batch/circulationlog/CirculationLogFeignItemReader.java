package org.folio.dew.batch.circulationlog;

import java.util.UUID;
import org.folio.dew.client.AuditClient;
import org.folio.dew.domain.dto.LogRecord;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

public class CirculationLogFeignItemReader
    extends AbstractItemCountingItemStreamItemReader<LogRecord> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 100;
  private static final boolean IS_SAVE_READER_STATE = false;

  private final AuditClient auditClient;
  private int currentOffset;

  private LogRecord[] currentChunk;
  private int currentChunkOffset;

  public CirculationLogFeignItemReader(AuditClient auditClient, int offset, int limit) {
    this.auditClient = auditClient;
    currentOffset = offset;

    setCurrentItemCount(0);
    setMaxItemCount(limit);
    setSaveState(IS_SAVE_READER_STATE);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected LogRecord doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.length) {
      currentChunk = getLogRecord();
      currentOffset += QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST;
      currentChunkOffset = 0;
    }

    if (currentChunk.length == 0) {
      return null;
    }

    LogRecord logRecord = currentChunk[currentChunkOffset];
    currentChunkOffset++;

    return logRecord;
  }

  @Override
  protected void doOpen() {}

  @Override
  protected void doClose() {}

  private LogRecord[] getLogRecord() {
    return auditClient
        .getCirculationAuditLogs(null, currentOffset, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST, null)
        .getLogRecords()
        .toArray(new LogRecord[0]);
  }
}
