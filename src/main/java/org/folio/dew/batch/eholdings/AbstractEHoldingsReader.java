package org.folio.dew.batch.eholdings;

import java.util.List;
import java.util.UUID;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

public abstract class AbstractEHoldingsReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

  private final int quantityToRetrievePerHttpRequest;
  private T lastObject;
  private List<T> currentChunk;
  private int currentChunkOffset;

  protected AbstractEHoldingsReader(T lastObject, Long limit, Integer perRequest) {
    this.lastObject = lastObject;
    quantityToRetrievePerHttpRequest = perRequest;

    setCurrentItemCount(0);
    setMaxItemCount(limit.intValue());
    setSaveState(false);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected T doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getItems(lastObject, quantityToRetrievePerHttpRequest);
      lastObject = currentChunk.get(currentChunk.size() - 1);
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

  protected abstract List<T> getItems(T last, int limit);
}
