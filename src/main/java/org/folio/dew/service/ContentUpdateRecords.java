package org.folio.dew.service;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ContentUpdateRecords<T> {

  private List<T> updated = new ArrayList<>();
  private List<T> preview = new ArrayList<>();

  public void addToUpdated(T entity) {
    updated.add(entity);
  }

  public void addToPreview(T entity) {
    preview.add(entity);
  }
}
