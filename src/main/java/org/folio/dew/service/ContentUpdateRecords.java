package org.folio.dew.service;

import lombok.Getter;
import org.folio.dew.domain.dto.ItemFormat;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ContentUpdateRecords {

  private List<ItemFormat> updated = new ArrayList<>();
  private List<ItemFormat> preview = new ArrayList<>();

  public void addToUpdated(ItemFormat itemFormat) {
    updated.add(itemFormat);
  }

  public void addToPreview(ItemFormat itemFormat) {
    preview.add(itemFormat);
  }
}
