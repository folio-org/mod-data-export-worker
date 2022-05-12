package org.folio.dew.batch;

import java.util.List;
import java.util.UUID;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

public abstract class CsvItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

  private final int quantityToRetrievePerHttpRequest;
  private int currentOffset;

  private List<T> currentChunk;
  private int currentChunkOffset;

  protected CsvItemReader(Long offset, Long limit, Integer perRequest) {
    currentOffset = offset.intValue();
    quantityToRetrievePerHttpRequest = perRequest;

    setCurrentItemCount(0);
    setMaxItemCount(limit.intValue());
    setSaveState(false);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected T doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getItems(currentOffset, quantityToRetrievePerHttpRequest);
      currentOffset += quantityToRetrievePerHttpRequest;
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
    // Nothing to do
  }

  @Override
  protected void doClose() {
    // Nothing to do
  }

  protected abstract List<T> getItems(int offset, int limit);

}
