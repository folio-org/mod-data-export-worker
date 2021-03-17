package org.folio.dew.batch;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

import java.util.List;
import java.util.UUID;

public abstract class CsvItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 100;

  private int currentOffset;

  private List<T> currentChunk;
  private int currentChunkOffset;

  protected CsvItemReader(Long offset, Long limit) {
    currentOffset = offset.intValue();

    setCurrentItemCount(0);
    setMaxItemCount(limit.intValue());
    setSaveState(false);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected T doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getItems(currentOffset, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);
      currentOffset += QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST;
      currentChunkOffset = 0;
    }

    if (currentChunk.isEmpty()) {
      return null;
    }

    T item = currentChunk.get(currentChunkOffset);
    currentChunkOffset++;

    return item;
  }

  @Override
  protected void doOpen() {
    // Nothing to do
  }

  @Override
  protected void doClose() {
    // Nothing to do
  }

  protected abstract List<T> getItems(int offset, int limit);

}
