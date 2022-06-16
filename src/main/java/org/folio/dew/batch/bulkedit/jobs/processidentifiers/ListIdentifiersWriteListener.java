package org.folio.dew.batch.bulkedit.jobs.processidentifiers;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ListIdentifiersWriteListener<T> implements ItemWriteListener<List<T>> {
  private final IdentifiersWriteListener<T> delegate;

  @Override public void beforeWrite(List<? extends List<T>> list) {
    // no implementation required
  }

  @Override public void afterWrite(List<? extends List<T>> list) {
    delegate.afterWrite(list.stream().flatMap(List::stream).collect(Collectors.toList()));
  }

  @Override public void onWriteError(Exception e, List<? extends List<T>> list) {
    // no implementation required
  }
}
