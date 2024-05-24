package org.folio.dew.batch;

import org.springframework.batch.item.file.transform.LineAggregator;

import java.util.List;

public class MrcFileLineAggregator implements LineAggregator<List<String>> {

  private static final String GS = Character.toString(29);
  private static final String RS = Character.toString(30);

  @Override
  public String aggregate(List<String> item) {
    return String.join(RS + GS, item);
  }
}
