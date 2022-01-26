package org.folio.dew.batch.acquisitions.edifact.jobs;

import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;

import java.util.Map;

public class EdifactExportCqlPartitioner implements Partitioner {
  public EdifactExportCqlPartitioner(Long offset, Long limit, String query) {

  }

  @Override public Map<String, ExecutionContext> partition(int gridSize) {
    return null;
  }
}
