package org.folio.dew.batch.bulkedit.jobs.processquery.items;

import org.folio.dew.batch.CsvPartitioner;
import org.folio.dew.client.InventoryClient;

public class BulkEditCqlItemPartitioner extends CsvPartitioner {

  private final InventoryClient inventoryClient;
  private final String query;

  public BulkEditCqlItemPartitioner(Long offset, Long limit, String tempOutputFilePath, InventoryClient inventoryClient, String query) {
    super(offset, limit, tempOutputFilePath);

    this.inventoryClient = inventoryClient;
    this.query = query;
  }

  @Override
  protected Long getLimit() {
    return Long.valueOf(inventoryClient.getItemByQuery(query, 0, 1).getTotalRecords());
  }

}
