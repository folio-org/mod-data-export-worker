package org.folio.dew.batch.eholdings;

import java.util.List;
import java.util.UUID;

import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;

public abstract class AbstractEHoldingsReader<T> extends AbstractItemCountingItemStreamItemReader<T> {

  private final int quantityToRetrievePerRequest;
  private T lastObject;
  private List<T> currentChunk;
  private int currentChunkOffset;

  protected AbstractEHoldingsReader(T lastObject, Long limit, Integer perRequest) {
    this.lastObject = lastObject;
    quantityToRetrievePerRequest = perRequest;

    setCurrentItemCount(0);
    setMaxItemCount(limit.intValue());
    setSaveState(false);
    setExecutionContextName(getClass().getSimpleName() + '_' + UUID.randomUUID());
  }

  @Override
  protected T doRead() {
    if (currentChunk == null || currentChunkOffset >= currentChunk.size()) {
      currentChunk = getItems(lastObject, quantityToRetrievePerRequest);
      lastObject = currentChunk.get(currentChunk.size() - 1);
      currentChunkOffset = 0;
    }

    return currentChunk.isEmpty() ? null : currentChunk.get(currentChunkOffset++);
  }

  protected abstract List<T> getItems(T last, int limit);
}
