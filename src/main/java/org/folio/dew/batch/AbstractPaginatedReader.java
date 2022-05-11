package org.folio.dew.batch;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

import java.util.List;
import java.util.UUID;

public abstract class AbstractPaginatedReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

  private static final int READER_HTTP_REQUEST_QUANTITY = 20;

  private int currentOffset;

  private List<T> currentChunk;
  private int currentChunkOffset;

  protected AbstractPaginatedReader() {
    currentOffset = 0;
    setCurrentItemCount(0);
    setSaveState(false);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected T doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getItems(currentOffset, READER_HTTP_REQUEST_QUANTITY);
      currentOffset += READER_HTTP_REQUEST_QUANTITY;
      currentChunkOffset = 0;
    }
    if (currentChunk.isEmpty()) {
      return null;
    }
    var item = currentChunk.get(currentChunkOffset);
    currentChunkOffset++;
    return item;
  }

  @Override
  protected void doOpen() {
    setMaxItemCount(getTotalCount());
  }

  @Override
  protected void doClose() {
    // Nothing to do
  }

  protected abstract List<T> getItems(int offset, int limit);

  protected abstract int getTotalCount();
}
