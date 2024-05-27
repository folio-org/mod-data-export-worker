package org.folio.dew.batch;

import org.springframework.batch.item.file.transform.LineAggregator;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class MrcFileLineAggregator implements LineAggregator<List<String>> {

  @Override
  public String aggregate(List<String> item) {
    return String.join(EMPTY, item);
  }
}
