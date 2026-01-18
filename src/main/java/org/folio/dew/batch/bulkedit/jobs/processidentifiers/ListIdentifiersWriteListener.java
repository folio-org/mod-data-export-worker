package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.Chunk;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ListIdentifiersWriteListener<T> implements ItemWriteListener<List<T>> {
  private final IdentifiersWriteListener<T> delegate;

  @Override public void afterWrite(Chunk<? extends List<T>> list) {
    var chunk = new Chunk<>(list.getItems().stream().flatMap(List::stream).toList());
    delegate.afterWrite(chunk);
  }
}
