package org.folio.dew.batch.bulkedit.jobs.processquery.items;

import java.util.List;
import org.folio.dew.batch.CsvItemReader;
import org.folio.dew.client.InventoryClient;
import org.folio.dew.domain.dto.ExtendedItem;

/**
 @deprecated  To remove - cql query isn't supported
 */
@Deprecated(forRemoval = true)
public class BulkEditCqlItemReader extends CsvItemReader<ExtendedItem> {

  private static final int QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST = 100;

  private final InventoryClient inventoryClient;
  private final String query;

  public BulkEditCqlItemReader(InventoryClient inventoryClient, String query, Long offset, Long limit) {
    super(offset, limit, QUANTITY_TO_RETRIEVE_PER_HTTP_REQUEST);

    this.inventoryClient = inventoryClient;
    this.query = query;
  }

  @Override
  @Deprecated
  protected List<ExtendedItem> getItems(int offset, int limit) {
    return inventoryClient.getItemByQuery(query, offset, limit).getItems().stream().map(item -> new ExtendedItem().entity(item).tenantId(null)).toList();
  }

}
